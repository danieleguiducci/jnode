/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.http;

import java.util.concurrent.CompletableFuture;
import org.jnode.core.JNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author daniele
 */
public class Http {
    private final static Logger log = LoggerFactory.getLogger(Http.class);
    public static NHttpServer createServer(NHttpServerHandler listener) {
        return new NHttpServer(listener);
    }
    public static CompletableFuture<NHttpServer> createServerStatus(int port) {
        
        return new NHttpServer((req,resp) -> {
            resp.addHeader("Content-Type","text/html");
            StringBuilder sb=new StringBuilder();
            sb.append("<html><head><title>Server status and load</title></head><body>Server list:<br/><ul>");
            JNode.get().getLoopers().forEach(looper->{
                sb.append("<li> Looper ID: ").append(looper.getId()).append(" Load:").append(looper.getLoadFactor()).append("</li>");
            });
            sb.append("</ul></body></html>");
            resp.end(sb.toString());
            
        }).listen(port).whenComplete((ok,ex)->{
            if(ok!=null) {
                log.info("Http Server status is online on port {}",ok.getLocalPort());
            } else {
                log.error("Errore on creation server ",ex);
            }
        });
    }
}
