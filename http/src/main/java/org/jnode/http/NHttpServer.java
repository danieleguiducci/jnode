/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.http;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.jnode.net.NServerSocket;
import org.jnode.net.NSocket;
import org.jnode.net.NSocketServerHandler;
import org.jnode.net.Net;
import org.jnode.net.OnErrorHandler;
import org.apache.http.HttpException;
import org.apache.http.ProtocolException;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestParser;
import org.apache.http.message.BasicHttpRequest;
import org.jnode.core.JNode;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class NHttpServer {
    
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(NHttpServer.class);
    private NHttpServerHandler handler;
    private NServerSocket server;
    private OnErrorHandler errorHandler;
    protected NHttpServer(NHttpServerHandler handler) {
        if(handler==null) throw new NullPointerException();
        this.server=Net.createServer(new NServerHandler());
        this.handler=handler;
    };
    private class NServerHandler implements NSocketServerHandler {

        @Override
        public void incomingConnection(NSocket nsc) {
            final NSessionInputBuffer buffer=new NSessionInputBuffer(1000);
            final DefaultHttpRequestParser parser=new DefaultHttpRequestParser(buffer);
            
            nsc.onData(sock->{
                int letti=buffer.fill(nsc);
                try {
                    Object o=parser.parse();
                    
                    if(o!=null) {
                        NHttpResponse resp=new NHttpResponse(sock);
                        handler.incomingRequest((BasicHttpRequest)o, resp);
                    } 
                } catch(ProtocolException ex) {
                    sock.close();
                    _onError(ex);
                } catch (IOException | HttpException ex) {
                    sock.close();
                    _onError(ex);
                }
            });
        }

   
    }
    public JNode getJNode() {
        return this.server.getJNode();
    }
    public int getLocalPort() {
        return this.server.getLocalPort();
    }
    public CompletableFuture<NHttpServer> listen(int port) {
        
        return server.listen(port).thenCompose((el)->CompletableFuture.completedFuture(this));
    }
    private void _onError(Exception bb) {
        if (errorHandler == null) 
            return;
        try {
            errorHandler.onError(bb);
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }
    
    public void onError(OnErrorHandler handler) {
        this.errorHandler=handler;
        server.onError(handler);
    }
    
}
