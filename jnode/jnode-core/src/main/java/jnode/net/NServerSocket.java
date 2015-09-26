/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jnode.core.JNodeCore;
import jnode.net.NSocketServerHandler;
import jnode.net.OnErrorHandler;

/**
 *
 * @author daniele
 */
public class NServerSocket {
    private static final Logger log = Logger.getLogger(NServerSocket.class.getName());
    private boolean isBound = false;
    private ServerSocketChannel ssc;
    private OnErrorHandler errorHandler;
    private NSocketServerHandler listener;
    private JNodeCore jnode;
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
            log.log(Level.FINEST, "Creating socket server");
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ssc.socket().bind(new InetSocketAddress(port));
            log.log(Level.FINEST, "Registering on selector");
            jnode.register(ssc, SelectionKey.OP_ACCEPT, new ServerChannelEvent());
            this.ssc = ssc;
            isBound = true;
            log.log(Level.FINE, "Server is created on port {0}", port);
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
