/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import org.jnode.core.JNode;
import org.jnode.core.JNode.RegisterResult;
import org.jnode.core.Looper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class NSocket implements Closeable{
    private final static Logger log = LoggerFactory.getLogger(Looper.class);
    private SocketChannel sc;
    private OnErrorHandler errorHandler;
    private OnDataHandler dataHandler;
    private onDrainHandler drainHandler;
    private OnCloseHandler closeHanlder;
    private SelectionKey sk;
    private final LinkedList<ByteBuffer> sendingBuffer = new LinkedList<>();
    private final JNode jnode;
    private Looper looper;
    private boolean isConnected=true;
    private static final Charset charset=Charset.forName("utf-8");
    protected NSocket(JNode jnode) {
        this.jnode=jnode;
    }
    private CompletableFuture<SocketChannel> configure(SocketChannel sc) {
        CompletableFuture<SocketChannel> cf=new CompletableFuture<>();
        try {
            sc.configureBlocking(false);
            sc.setOption(StandardSocketOptions.TCP_NODELAY,true);
            cf.complete(sc);
        } catch(Exception e){
            cf.completeExceptionally(e);
        }
        return cf;
    }
    protected CompletableFuture<Void> setSocketChannel(SocketChannel sc) {
        this.sc = sc;
        return configure(sc).thenCompose((sock)->jnode.register(sc,  SelectionKey.OP_READ, new SocketChannelEvent()))
                .thenAccept((RegisterResult rr) -> {
                    sk=rr.sk;
                    looper=rr.assignedLooper;
                });
        
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
    
    private void _close() {
        sendConnectionDown();
        try {
            sc.close();
        } catch (IOException ex) {
            looper.schedule(()-> {_onError(ex);}) ;
        }
    }

    @Override
    public void close() {
        if(Thread.currentThread().getId()==looper.getId())
            _close();
        else
            looper.schedule(()->{this._close();});
    }
    private void sendConnectionDown() {
        if(!isConnected) return;
        sk.cancel();
        isConnected=false;
        looper.schedule(()-> {_onClose();}) ;
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
            looper.schedule(()-> { _onError(ex);}) ;
            return 0;
        }
        
    }
    public void write(String data) {
        write(ByteBuffer.wrap(data.getBytes(charset)));
    }
    private void _write(ByteBuffer bb) {
        if (bb.remaining() == 0) 
            return;
        if (!sk.isValid()) {
            _onError(new IOException("Sk is not valid"));
            return;
        }
        if(sendingBuffer.isEmpty()) {
            try {
                sc.write(bb);
            } catch (IOException ex) {
                sendConnectionDown();
                return;
            }
        }
        if(bb.hasRemaining()) {
            sendingBuffer.add(bb);
            sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
        } else {
            _onDrain();
        }
    }
    public void write(final ByteBuffer bb) {
        if(Thread.currentThread().getId()==looper.getId())
            _write(bb);
        else
            looper.schedule(() -> {this._write(bb);});
        
    }
    public boolean pendingData () {
        return !this.sendingBuffer.isEmpty();
    }
    private void _onClose() {
        if (closeHanlder == null) 
            return;
        try {
            closeHanlder.onClose();
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }

    private void _onError(Exception bb) {
        if (errorHandler == null) {
            return;
        }
        try {
            errorHandler.onError(bb);
        } catch (Throwable t) {
            log.error( "Uncatched error", t);
        }
    }

    private void _onData() {
        if (dataHandler == null) {
            return;
        }
        try {
            dataHandler.onDataIncoming(this);
        } catch (Throwable t) {
            log.error( "Uncatched error", t);
        }
    }
    
    private void _onDrain() {
        if (drainHandler == null) {
            return;
        }
        try {
            drainHandler.onDrain();
        } catch (Throwable t) {
            log.error( "Uncatched error", t);
            
        }
    }

    private class SocketChannelEvent implements Looper.ChannelEvent {

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
