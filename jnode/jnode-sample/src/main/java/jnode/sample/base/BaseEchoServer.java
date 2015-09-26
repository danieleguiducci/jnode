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
import jnode.core.Loop;
import jnode.core.Loop.NServerSocketChannel;

/**
 *
 * @author daniele
 */
public class BaseEchoServer {

    private static final Logger log = Logger.getLogger(BaseEchoServer.class.getName());

    public static void main(String arg[]) throws IOException {
        LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
        log.fine("I'm starting the echo server");
        NServerSocketChannel nss = Loop.getLoop().createServer(socket -> {
            System.out.println("Incoming connection");
            socket.onData((ByteBuffer data) -> {
                String string = new String(data.array(), 0, data.remaining());
                data.flip();
                socket.write(data);
                log.log(Level.FINE, "Incoming data:{0}", string);
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
        Loop.getLoop().loop();
    }

}
