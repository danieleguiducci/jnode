/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.jersey;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import javax.ws.rs.core.Request;
import org.apache.http.Header;
import org.apache.http.message.BasicHttpRequest;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.ContainerUtils;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
import org.jnode.http.Http;
import org.jnode.http.NHttpResponse;
import org.jnode.http.NHttpServer;
import org.jnode.http.NHttpServerHandler;
import org.slf4j.LoggerFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import java.lang.reflect.Type;
import javax.ws.rs.core.Application;
import org.glassfish.hk2.api.TypeLiteral;
/**
 *
 * @author daniele
 */
public class JNContainer implements Container, NHttpServerHandler {

    private final static org.slf4j.Logger log = LoggerFactory.getLogger(JNContainer.class);
    private ApplicationHandler handler;
    private NHttpServer httpServer;

    public JNContainer(ResourceConfig res) {
        handler = new ApplicationHandler(res);

    }
     private final Type RequestTYPE = (new TypeLiteral<Ref<BasicHttpRequest>>() {}).getType();
    private final Type ResponseTYPE = (new TypeLiteral<Ref<NHttpResponse>>() { }).getType();
    @Override
    public void incomingRequest(BasicHttpRequest req, NHttpResponse res) {
        try {
            log.trace("Incoming request");
            final URI baseUri = getBaseUri(req);
            final URI requestUri = getRequestUri(req, baseUri);
            ContainerRequest requestContext = new ContainerRequest(baseUri,
                    requestUri, req.getRequestLine().getMethod(), null, new MapPropertiesDelegate());
            requestContext.setEntityStream(new ByteArrayInputStream( "".getBytes() ));
            Arrays.stream(req.getAllHeaders()).forEach((Header he)->{
                requestContext.headers(he.getName(), he.getValue());
            });
            requestContext.setWriter(new Writer(res));
            requestContext.setRequestScopedInitializer(new RequestScopedInitializer() {

                @Override
                public void initialize(ServiceLocator locator) {
                  //  locator.<Ref<BasicHttpRequest>>getService(RequestTYPE).set(req);
                  //  locator.<Ref<NHttpResponse>>getService(ResponseTYPE).set(res);
                }
            });
            requestContext.setSecurityContext(new BaseSecurityContext());
            handler.handle(requestContext);
        } catch (Throwable t) {
            log.error("Unhandle exception ", t);
            res.end();
        }
    }

    private URI getRequestUri(BasicHttpRequest request, final URI baseUri) {
        try {
            final String serverAddress = getServerAddress(baseUri);
            String uri = ContainerUtils.getHandlerPath(request.getRequestLine().getUri());
            final String queryString = "";
            if (queryString != null) {
                uri = uri + "?" + ContainerUtils.encodeUnsafeCharacters(queryString);
            }

            return new URI(serverAddress + uri);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private String getServerAddress(final URI baseUri) throws URISyntaxException {
        return new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(), null, null, null).toString();
    }

    private URI getBaseUri(BasicHttpRequest request) {
        try {
            Header hostHeader = request.getFirstHeader("host");

            if (hostHeader != null) {
                final String scheme = "http";
                return new URI(scheme + "://" + hostHeader.getValue() + "/");
            } else {
                return new URI("http", null, "localhost", 80, "/", null, null);
            }
        } catch (final URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public void start() {
        NHttpServer httpServer = Http.createServer(this);
        httpServer.listen(DEFAULT_HTTP_PORT).whenComplete((ok, ex) -> {
            if (ex != null)
                log.error("Errore. Can't bind server to tcp port", ex);

        });
        handler.onStartup(this);

    }

    public void stop() {
        handler.onShutdown(this);
    }

    @Override
    public ResourceConfig getConfiguration() {
        return handler.getConfiguration();
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return handler;
    }

    @Override
    public void reload() {
        reload(getConfiguration());
    }

    @Override
    public void reload(ResourceConfig rc) {
        handler.onShutdown(this);
        handler = new ApplicationHandler();
        handler.onReload(this);
        handler.onStartup(this);

    }

}
