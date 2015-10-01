/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.jersey;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.jnode.http.NHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
 public class Writer implements ContainerResponseWriter {
        private final static Logger log = LoggerFactory.getLogger(Writer.class);
        private final NHttpResponse response;

        public Writer(final NHttpResponse response) {
            this.response = response;
        }

        @Override
        public OutputStream writeResponseStatusAndHeaders(final long contentLength, final ContainerResponse context)
                throws ContainerException {
            final javax.ws.rs.core.Response.StatusType statusInfo = context.getStatusInfo();

            final int code = statusInfo.getStatusCode();
            response.setCode(code);
            for (final Map.Entry<String, List<String>> e : context.getStringHeaders().entrySet()) {
                for (final String value : e.getValue()) {
                    response.addHeader(e.getKey(), value);
                }
            }
            return response;

        }

        @Override
        public boolean suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
            throw new UnsupportedOperationException("Method suspend is not supported by the container.");
        }

        @Override
        public void setSuspendTimeout(final long timeOut, final TimeUnit timeUnit) throws IllegalStateException {
            throw new UnsupportedOperationException("Method suspend is not supported by the container.");
        }

        @Override
        public void commit() {
            response.close();
        }

        @Override
        public void failure(final Throwable error) {
            log.error("Error",error);

        }

        @Override
        public boolean enableResponseBuffering() {
            return false;
        }

        /**
         * Rethrow the original exception as required by JAX-RS, 3.3.4
         *
         * @param error throwable to be re-thrown
         */
        private void rethrow(final Throwable error) {
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            } else {
                throw new ContainerException(error);
            }
        }

    }
