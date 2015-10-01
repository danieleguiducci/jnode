/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.sample.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import org.jnode.net.NServerSocket;
import org.jnode.net.NSocket;
import org.jnode.net.Net;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class Chat {

    private final static Logger log = LoggerFactory.getLogger(Chat.class);

    public static void main(String arg[]) throws IOException {
        log.info("I'm starting the echo server");
        HashSet<NSocket> con=new HashSet<>();
        NServerSocket nss = Net.createServer((NSocket socket) -> {
            log.trace("Incoming connection");
            con.add(socket);
            socket.out.println("Welcome in the Chat server.");
            socket.out.flush();
            ByteBuffer bb = ByteBuffer.allocate(200);
            socket.onData(sock -> {
                bb.clear();
                int letti=sock.read(bb);
                con.forEach((altro)->{
                    if(altro==sock)return;
                    ByteBuffer toSend = ByteBuffer.allocate(200); // it's evil
                    bb.flip();
                    toSend.put(bb);
                    toSend.flip();
                    altro.executeSafe(()->{
                        altro.out.write(toSend);
                    });
                });
                
            });
            socket.onClose(() -> {
                log.trace("Connection lost");
                con.remove(socket);
            });
        });
        nss.listen(54321).handle((ok, ex) -> {
            if (ex != null) {
                log.error("Binding error ", ex);
            } else {
                log.info("Server is ready to accept connection");
            }
            return -1;
        });
        nss.onError(ex -> {
            log.error("Errore on accepting connection ");
        });

    }
}
