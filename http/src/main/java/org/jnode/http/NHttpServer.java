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
import org.jnode.net.OnCloseHandler;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class NHttpServer {

    private final static org.slf4j.Logger log = LoggerFactory.getLogger(NHttpServer.class);
    private NHttpServerHandler httpReqHandler;
    private NServerSocket server;
    private OnErrorHandler errorHandler;

    protected NHttpServer(NHttpServerHandler httpReqHandler) {
        if (httpReqHandler == null)
            throw new NullPointerException();
        server = Net.createServer(new NServerHandler());
        this.httpReqHandler = httpReqHandler;
        server.onError((ex) -> {
            _onError(ex);
        });
    }

    ;
    private class NServerHandler implements NSocketServerHandler {

        @Override
        public void incomingConnection(NSocket nsc) {
            final NSessionInputBuffer buffer = new NSessionInputBuffer(1000);
            final DefaultHttpRequestParser parser = new DefaultHttpRequestParser(buffer);
            nsc.onData(sock -> {

                int letti = buffer.fill(nsc);
                if (letti == -1)
                    return;
                try {
                    Object o = parser.parse();

                    if (o != null) {
                        NHttpResponse resp = new NHttpResponse(sock);
                        httpReqHandler.incomingRequest((BasicHttpRequest) o, resp);
                    }
                } catch (ProtocolException ex) {
                    sock.close();
                    _onError(ex);
                } catch (IOException | HttpException ex) {
                    sock.close();
                    _onError(ex);
                }
            });
            nsc.onClose((sock) -> {
                _onClose(sock);
            });
            nsc.onError((ex) -> {
                _onError(ex);
            });

        }

    }

    private void _onClose(NSocket sock) {
        if (closeHandler == null)
            return;
        try {
            closeHandler.onClose(sock);
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }
    private OnCloseHandler closeHandler;

    public void onSocketClose(OnCloseHandler handler) {
        closeHandler = handler;
    }

    public JNode getJNode() {
        return this.server.getJNode();
    }

    public int getLocalPort() {
        return this.server.getLocalPort();
    }

    public CompletableFuture<NHttpServer> listen(int port) {

        return server.listen(port).thenCompose((el) -> CompletableFuture.completedFuture(this));
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

    public void onError(OnErrorHandler handler) {
        this.errorHandler = handler;
    }

}
