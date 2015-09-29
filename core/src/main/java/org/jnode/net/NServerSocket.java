/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import org.jnode.core.JNode;
import org.jnode.core.JNode.RegisterResult;
import org.jnode.core.Looper;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author daniele
 */
public class NServerSocket implements Closeable {

    private final static org.slf4j.Logger log = LoggerFactory.getLogger(NServerSocket.class);
    private boolean isBound = false;
    private ServerSocketChannel ssc;
    private OnErrorHandler errorHandler;
    private NSocketServerHandler listener;
    private JNode jnode;
    private Looper looper;
    private SelectionKey sk;

    protected NServerSocket(JNode jnode, NSocketServerHandler listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener can't be null");
        }
        this.jnode = jnode;
        this.listener = listener;
    }

    public CompletableFuture<NServerSocket> listen(int port) {

        if (isBound) {
            CompletableFuture cf = new CompletableFuture<>();
            cf.completeExceptionally(new IllegalStateException("Server already bound"));
            return cf;
        }
        return createServerChannel(port)
                .thenCompose((socketChannel) -> {
                    this.ssc = socketChannel;
                    return jnode.register(socketChannel, SelectionKey.OP_ACCEPT, new ServerChannelEvent());
                }).whenComplete((ok, ex) -> {
                    if (ex != null && ssc != null && ssc.isOpen())
                        IOUtils.closeQuietly(ssc);
                }).thenCompose((RegisterResult rr) -> {
                    sk = rr.sk;
                    looper = rr.assignedLooper;
                    isBound = true;
                    return CompletableFuture.completedFuture(this);
                });
    }
    public JNode getJNode() {
        return this.jnode;
    }
    public int getLocalPort() {
        return ssc.socket().getLocalPort();
    }
    private CompletableFuture<ServerSocketChannel> createServerChannel(int port) {
        CompletableFuture cf = new CompletableFuture<>();
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(port));
            cf.complete(ssc);
        } catch (Exception e) {
            cf.completeExceptionally(e);
        }
        return cf;
    }

    public void onError(OnErrorHandler handler) {
        this.errorHandler = handler;
    }

    @Override
    public void close() {
        if (ssc.isOpen() && sk.isValid()) {
            try {
                ssc.close();
            } catch (IOException ex) {
                looper.schedule(() -> {
                    _onError(ex);
                });
            }
        }
    }

    private void _onError(Throwable bb) {
        if (errorHandler == null)
            return;
        try {
            errorHandler.onError(bb);
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }

    private class ServerChannelEvent implements Looper.ChannelEvent {

        @Override
        public void onEvent(SelectionKey a) {
            if (a.isAcceptable()) {
                try {
                    SocketChannel sc = ssc.accept();
                    NSocket nsc = new NSocket(jnode);
                    nsc.setSocketChannel(sc).thenAccept((elem)-> {
                        try {
                            listener.incomingConnection(nsc);
                        } catch (Throwable t) {
                            log.error("Exception not handle", t);
                        }
                    }).whenComplete( (ok,ex) -> {
                        if(ex!=null)
                            _onError(ex);
                    });
                    
                } catch (IOException ex) {
                    _onError(ex);
                }
            }
        }
    }

}
