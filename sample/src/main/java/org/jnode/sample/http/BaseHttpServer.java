package org.jnode.sample.http;

import java.io.IOException;
import org.jnode.core.JNodeCore;
import org.jnode.http.Http;
import org.jnode.http.NHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class BaseHttpServer {

    private final static Logger log = LoggerFactory.getLogger(BaseHttpServer.class);

    public static void main(String[] args) throws IOException {
        log.info("I'm starting the http server");
        
        NHttpServer nhs=Http.createServer((req, resp)->{
            log.trace("Http request incoming. Url:{0}",req.getRequestLine().getUri());
            resp.addHeader("Content-Type","text/plain");
            resp.end("Hello world!");
        });
        nhs.listen(80).whenComplete((ok, ex)->{
            if(ex!=null) {
                log.error("Error binding to port ",ex);
            }
        });
        nhs.onError(ex -> {
            log.error("Socket error ",ex);
        });
        JNodeCore.get().loop();
    }
}
