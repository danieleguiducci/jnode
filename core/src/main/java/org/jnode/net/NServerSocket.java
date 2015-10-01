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
import java.util.concurrent.CompletableFuture;
import org.jnode.core.JNode;
import org.jnode.core.Looper;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.jnode.core.JNode.NContext;

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
    private Looper looper;
    private SelectionKey sk;
    private final NContext ncontext;
    public NServerSocket(NContext ncontext,NSocketServerHandler listener) {
        if(listener==null) throw new NullPointerException("invalid Null listener");
        this.ncontext=ncontext;
        looper=ncontext.requestLooper();
        this.listener=listener;
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
                    return looper.register(socketChannel, SelectionKey.OP_ACCEPT, new ServerChannelEvent());
                }).whenComplete((ok, ex) -> {
                    if (ex != null && ssc != null && ssc.isOpen())
                        IOUtils.closeQuietly(ssc);
                }).thenCompose((SelectionKey selKey) -> {
                    sk = selKey;
                    isBound = true;
                    return CompletableFuture.completedFuture(this);
                });
    }
    public JNode getJNode() {
        return this.ncontext.getJNode();
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

    public NServerSocket onError(OnErrorHandler handler) {
        this.errorHandler = handler;
        return this;
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
                NSocket nsc = new NSocket(ncontext);
                nsc.setSocketChannel(ssc).thenAccept((elem)-> {
                    nsc.executeSafe(()->{listener.incomingConnection(nsc);});
                }).whenComplete( (ok,ex) -> {
                    if(ex!=null)
                        _onError(ex);
                });
            }
        }
    }

}
