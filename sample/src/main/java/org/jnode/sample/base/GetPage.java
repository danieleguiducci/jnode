/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.sample.base;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import org.jnode.net.NSocket;
import org.jnode.net.Net;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class GetPage {

    private final static Logger log = LoggerFactory.getLogger(GetPage.class);

    public static void main(String arg[]) throws Exception {
        log.info("I'm starting client");
        Net.createSocket().connect(new InetSocketAddress("www.google.com", 80))
                .thenAccept((nsock) -> {
                    ByteBuffer bb = ByteBuffer.allocate(5000);
                    nsock.onError((ex) -> {
                        log.error("Error", ex);
                    });
                    nsock.onConnectionEstablished((ns) -> {
                        log.debug("Connection enstablished");
                        nsock.out.write("GET / HTTP/1.1\r\nhost:www.google.com\r\n\r\n".getBytes());
                        nsock.out.flush();
                    });
                    nsock.onData((ns) -> {
                        int len = ns.read(bb);
                        log.debug("data:" + new String(bb.array(), 0, len));
                    });
                    nsock.onClose((Void) ->{
                        log.debug("Connection closed");
                    });
                }).exceptionally((Throwable ex) -> {
                    log.error("Exception", ex);
                    return null;
                });

    }
}
