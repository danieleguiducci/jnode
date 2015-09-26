/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOException;
import jnode.core.net.NSocketServerListener;
import jnode.core.net.OnCloseHandler;
import jnode.core.net.OnDataHandler;
import jnode.core.net.OnErrorHandler;
import jnode.core.net.onDrainHandler;

/**
 *
 * @author daniele
 */
public class Loop {

    private static final Logger log = Logger.getLogger(Loop.class.getName());
    private static Loop sampleJNode;
    private final Selector selector;
    private boolean isRunning = true;

    static {

        try {
            sampleJNode = new Loop();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }

    }

    public Loop() throws IOException {
        selector = Selector.open();
    }

    public void halt() {
        isRunning = false;
        selector.wakeup();
    }

    public void loop() {
        while (isRunning) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    ChannelEvent ce = (ChannelEvent) sk.attachment();
                    ce.onEvent(sk);
                    it.remove();
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Fatal error", ex);
            }
        }
    }

    public static Loop getLoop() {
        if (sampleJNode == null) {
            throw new IllegalStateException("No jnode found");
        }
        return sampleJNode;
    }

    ;
    public NServerSocketChannel createServer(NSocketServerListener listener) {
        return new NServerSocketChannel(listener);
    }

    public class NServerSocketChannel {

        private boolean isBound = false;
        private ServerSocketChannel ssc;
        private OnErrorHandler errorHandler;
        private NSocketServerListener listener;

        private NServerSocketChannel(NSocketServerListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("Listener can't be null");
            }
            this.listener = listener;
        }

        public CompletableFuture listen(int port) {
            CompletableFuture cf = new CompletableFuture<>();
            if (isBound) {
                cf.completeExceptionally(new IllegalStateException("Server already bound"));
                return cf;
            }
            try {
                log.log(Level.FINEST, "Creating socket server");
                ServerSocketChannel ssc = ServerSocketChannel.open();
                ssc.configureBlocking(false);
                ssc.socket().bind(new InetSocketAddress(port));
                log.log(Level.FINEST, "Registering on selector");
                ssc.register(selector, SelectionKey.OP_ACCEPT, new ServerChannelEvent());
                this.ssc = ssc;
                isBound = true;
                log.log(Level.FINE, "Server is created on port " + port);
                cf.complete(this);
            } catch (IOException e) {
                log.log(Level.WARNING, "Creation server error", e);
                cf.completeExceptionally(e);
            }
            return cf;
        }

        public void onError(OnErrorHandler handler) {
            this.errorHandler = handler;
        }

        private class ServerChannelEvent implements ChannelEvent {

            @Override
            public void onEvent(SelectionKey a) {
                if (a.isAcceptable()) {
                    try {
                        SocketChannel sc = ssc.accept();
                        NSocketChannel nsc = new NSocketChannel(sc);
                        try {
                            listener.incomingConnection(nsc);
                        } catch (Throwable t) {
                            System.out.println("Eccezione del listener" + t.getMessage());
                        }
                    } catch (IOException ex) {
                        if (errorHandler != null) {
                            errorHandler.onError(ex);
                        }
                    }
                }
            }
        }
    }

    public class NSocketChannel {

        private final SocketChannel sc;
        private OnErrorHandler errorHandler;
        private OnDataHandler dataHandler;
        private onDrainHandler drainHandler;
        private OnCloseHandler closeHanlder;
        private final SelectionKey sk;
        private final LinkedList<ByteBuffer> sendingBuffer = new LinkedList<>();

        private NSocketChannel(SocketChannel sc) throws IOException {
            sc.configureBlocking(false);
            this.sc = sc;
            sk = sc.register(selector, SelectionKey.OP_READ, new SocketChannelEvent());
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

        public void close() throws IOException {
            sc.close();
        }

        public void write(ByteBuffer bb) {
            if (bb.remaining() == 0) {
                return;
            }
            if (!sk.isValid()) {
                _onError(new IOException("Sk is not valid"));
                return;
            }
            sendingBuffer.add(bb);
            sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
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

        private void _onData(ByteBuffer bb) {
            if (dataHandler == null) {
                return;
            }
            try {
                dataHandler.onDataIncoming(bb);
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
                log.log(Level.SEVERE, "Uncatched error", t);;
            }
        }

        private class SocketChannelEvent implements ChannelEvent {

            @Override
            public void onEvent(SelectionKey a) {
                if(!a.isValid()) return;
                if (a.isReadable()) {
                    try {
                        ByteBuffer bb = ByteBuffer.allocate(1024);
                        int readed = sc.read(bb);
                        if (readed == -1) {
                            _onClose();
                            a.cancel();
                        } else {
                            _onData(bb);
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
                            if (drainHandler != null) {
                                drainHandler.onDrain();
                            }
                        }
                    } catch (IOException e) {
                        sk.cancel();
                        _onDrain();
                    }

                }
            }
        }
    }

    @FunctionalInterface
    private interface ChannelEvent {

        void onEvent(SelectionKey a);
    }

}
