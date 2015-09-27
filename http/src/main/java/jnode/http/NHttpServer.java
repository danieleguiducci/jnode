/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.http;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jnode.net.NServerSocket;
import jnode.net.NSocket;
import jnode.net.NSocketServerHandler;
import jnode.net.Net;
import jnode.net.OnErrorHandler;
import org.apache.http.HttpException;
import org.apache.http.ProtocolException;
import org.apache.http.impl.nio.codecs.DefaultHttpRequestParser;
import org.apache.http.message.BasicHttpRequest;

/**
 *
 * @author daniele
 */
public class NHttpServer {
    private static final Logger log = Logger.getLogger(NHttpServer.class.getName());
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
            //log.log(Level.FINE,"Incoming server connection");
            final NSessionInputBuffer buffer=new NSessionInputBuffer(100);
            final DefaultHttpRequestParser parser=new DefaultHttpRequestParser(buffer);
            
            nsc.onClose(()->{
                
            });
            nsc.onData(sock->{
                buffer.fill(nsc);
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
    public CompletableFuture listen(int port) {
        return server.listen(port);
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
    
    public void onError(OnErrorHandler handler) {
        this.errorHandler=handler;
        server.onError(handler);
    }
    
}
