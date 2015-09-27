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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jnode.core.JNodeCore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class NServerSocket implements Closeable{
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(NServerSocket.class);
    private boolean isBound = false;
    private ServerSocketChannel ssc;
    private OnErrorHandler errorHandler;
    private NSocketServerHandler listener;
    private JNodeCore jnode;
    private SelectionKey sk;
    protected NServerSocket(JNodeCore jnode,NSocketServerHandler listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener can't be null");
        }
        this.jnode=jnode;
        this.listener = listener;
    }

    public CompletableFuture listen(int port) {
        CompletableFuture cf = new CompletableFuture<>();
        if (isBound) {
            cf.completeExceptionally(new IllegalStateException("Server already bound"));
            return cf;
        }
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(port));
            sk=jnode.register(ssc, SelectionKey.OP_ACCEPT, new ServerChannelEvent());
            this.ssc = ssc;
            isBound = true;
            cf.complete(this);
        } catch (IOException e) {
            cf.completeExceptionally(e);
        }
        return cf;
    }

    public void onError(OnErrorHandler handler) {
        this.errorHandler = handler;
    }

    @Override
    public void close()  {
        if(ssc.isOpen() && sk.isValid()) {
            try {
                ssc.close();
            } catch (IOException ex) {
                jnode.schedule(()->{_onError(ex);});
            }
        }
    }
    private void _onError(Exception bb) {
        if (errorHandler == null) return;
        try {
            errorHandler.onError(bb);
        } catch (Throwable t) {
            log.error( "Uncatched error", t);
        }
    }
    private class ServerChannelEvent implements JNodeCore.ChannelEvent {

        @Override
        public void onEvent(SelectionKey a) {
            if (a.isAcceptable()) {
                try {
                    SocketChannel sc = ssc.accept();
                    NSocket nsc = new NSocket(jnode,sc);
                    try {
                        listener.incomingConnection(nsc);
                    } catch (Throwable t) {
                        log.error("Exception not handle",t);
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
