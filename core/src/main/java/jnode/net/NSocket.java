/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.net;

import java.io.IOException;
import java.net.SocketOption;
import java.net.SocketOptions;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jnode.core.JNodeCore;
import jnode.net.OnCloseHandler;
import jnode.net.OnDataHandler;
import jnode.net.OnErrorHandler;
import jnode.net.onDrainHandler;

/**
 *
 * @author daniele
 */
public class NSocket {
    private static final Logger log = Logger.getLogger(NSocket.class.getName());
    private final SocketChannel sc;
    private OnErrorHandler errorHandler;
    private OnDataHandler dataHandler;
    private onDrainHandler drainHandler;
    private OnCloseHandler closeHanlder;
    private final SelectionKey sk;
    private final LinkedList<ByteBuffer> sendingBuffer = new LinkedList<>();
    private final JNodeCore jnode;
    private boolean isConnected=true;
    protected NSocket(JNodeCore jnode,SocketChannel sc) throws IOException {
        sc.configureBlocking(false);
        this.sc = sc;
        this.jnode=jnode;
        sc.setOption(StandardSocketOptions.TCP_NODELAY,true);
        sk=jnode.register(sc,  SelectionKey.OP_READ, new SocketChannelEvent());
        
    }

    public void onError(OnErrorHandler handler) {
        this.errorHandler = handler;
    }

    public void onClose(OnCloseHandler handler) {
        this.closeHanlder = handler;
    }

    public void onData(OnDataHandler handler) {
        this.dataHandler = handler;
    }

    public void onDrain(onDrainHandler handler) {
        this.drainHandler = handler;
    }

    public CompletableFuture close() {
        CompletableFuture cf = new CompletableFuture<>();
        sendConnectionDown();
        try {
            
            sc.close();
        } catch (IOException ex) {
            jnode.schedule(()-> {_onError(ex);}) ;
        }
        return cf;
    }
    private void sendConnectionDown() {
        if(!isConnected) return;
        sk.cancel();
        isConnected=false;
        jnode.schedule(()-> {_onClose();}) ;
    }
    public ByteBuffer read() {
        ByteBuffer bb=ByteBuffer.allocate(128);
        int n=read(bb);
        return bb;
    }
    public int read(ByteBuffer bb)  {
        if(!isConnected) return 0;
        if(!sc.isConnected()) {
            sendConnectionDown();
            return 0;
        }
        try {
            int readed = sc.read(bb);
            if(readed==-1) {
                sendConnectionDown();
                return 0;
            }
            return readed;
        } catch (IOException ex) {
            jnode.schedule(()-> { _onError(ex);}) ;
            return 0;
        }
        
    }
    public void write(String data) {
        write(ByteBuffer.wrap(data.getBytes()));
    }
    public void write(ByteBuffer bb) {
        if (bb.remaining() == 0) {
            return;
        }
        if (!sk.isValid()) {
            _onError(new IOException("Sk is not valid"));
            return;
        }
        if(sendingBuffer.isEmpty()) {
            try {
                sc.write(bb);
            } catch (IOException ex) {
                sk.cancel();
                sendConnectionDown();
            }
        }
        if(bb.hasRemaining()) {
            sendingBuffer.add(bb);
            sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
        } else {
            _onDrain();
        }
        
    }
    public boolean pendingData () {
        return (!this.sendingBuffer.isEmpty()) | sk.isWritable();
    }
    private void _onClose() {
        if (closeHanlder == null) {
            return;
        }
        try {
            closeHanlder.onClose();
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Uncatched error", t);
        }
    }

    private void _onError(Exception bb) {
        if (errorHandler == null) {
            return;
        }
        try {
            errorHandler.onError(bb);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Uncatched error", t);
        }
    }

    private void _onData() {
        if (dataHandler == null) {
            return;
        }
        try {
            dataHandler.onDataIncoming(this);
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Uncatched error", t);
        }
    }
    
    private void _onDrain() {
        if (drainHandler == null) {
            return;
        }
        try {
            drainHandler.onDrain();
        } catch (Throwable t) {
            log.log(Level.SEVERE, "Uncatched error", t);
            
        }
    }

    private class SocketChannelEvent implements JNodeCore.ChannelEvent {

        @Override
        public void onEvent(SelectionKey a) {
            if (!a.isValid()) {
                return;
            }
            if (a.isReadable()) {
                try {
                
                    if (!sc.isConnected()) {
                        _onClose();
                        a.cancel();
                    } else {
                        _onData();
                    }
                } catch (Exception e) {
                    _onError(e);
                }
            }
            if (a.isValid() && a.isWritable()) {
                try {
                    boolean hasSend = true;
                    while (hasSend && !sendingBuffer.isEmpty()) {
                        ByteBuffer bb = sendingBuffer.getFirst();
                        sc.write(bb);
                        if (bb.remaining() == 0) {
                            sendingBuffer.removeFirst();
                        } else {
                            hasSend = false;
                        }
                    }
                    if (sendingBuffer.isEmpty()) {
                        sk.interestOps(sk.interestOps() & (~SelectionKey.OP_WRITE));
                        _onDrain();
                    }
                } catch (IOException e) {
                    sk.cancel();
                    sendConnectionDown();
                }
            }
        }
    }
    
}
