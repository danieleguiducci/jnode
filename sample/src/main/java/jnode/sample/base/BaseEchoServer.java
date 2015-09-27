/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.sample.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jnode.core.JNodeCore;
import jnode.net.NServerSocket;
import jnode.net.Net;

/**
 *
 * @author daniele
 */
public class BaseEchoServer {

    private static final Logger log = Logger.getLogger(BaseEchoServer.class.getName());

    public static void main(String arg[]) throws IOException {
        LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
        log.fine("I'm starting the echo server");
        NServerSocket nss = Net.createServer((jnode.net.NSocket socket) -> {
            log.fine("Incoming connection");
            socket.write("Welcome on the Echo Server\n\r");
            socket.onData(sock -> {
                ByteBuffer data=sock.read();
                data.flip();
                socket.write(data);
            });
            socket.onClose(() -> {
                log.log(Level.FINE, "Connection lost");
            });
        });
        nss.listen(54321).handle((ok, ex) -> {
            if (ex != null) {
                log.log(Level.SEVERE, "Binding error ", ex);
            } else {
                log.log(Level.INFO, "Server is ready to accept connection");
            }
            return -1;
        });
        nss.onError(ex -> {
            log.log(Level.SEVERE, "Errore on accepting connection ", ex);
        });
        JNodeCore.get().loop();
    }

}
