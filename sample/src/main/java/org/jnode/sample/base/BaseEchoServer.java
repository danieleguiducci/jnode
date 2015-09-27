/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.sample.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jnode.core.JNodeCore;
import org.jnode.net.NServerSocket;
import org.jnode.net.Net;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class BaseEchoServer {

    private final static Logger log = LoggerFactory.getLogger(BaseEchoServer.class);

    public static void main(String arg[]) throws IOException {
        log.info("I'm starting the echo server");
        NServerSocket nss = Net.createServer((org.jnode.net.NSocket socket) -> {
            log.trace("Incoming connection");
            socket.write("Welcome on the Echo Server\n\r");
            socket.onData(sock -> {
                ByteBuffer data=sock.read();
                data.flip();
                socket.write(data);
            });
            socket.onClose(() -> {
                log.trace( "Connection lost");
            });
        });
        nss.listen(54321).handle((ok, ex) -> {
            if (ex != null) {
                log.error("Binding error ",ex);
            } else {
                log.info("Server is ready to accept connection");
            }
            return -1;
        });
        nss.onError(ex -> {
            log.error("Errore on accepting connection ");
        });
        JNodeCore.get().loop();
    }

}
