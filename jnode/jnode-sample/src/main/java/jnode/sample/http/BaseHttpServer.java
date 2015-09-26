package jnode.sample.http;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import jnode.core.JNodeCore;
import jnode.http.Http;
import jnode.http.NHttpServer;
import org.apache.http.message.BasicHttpRequest;
public class BaseHttpServer {

    /**
     * @param args the command line arguments
     */
    private static final Logger log = Logger.getLogger(BaseHttpServer.class.getName());

    public static void main(String[] args) throws IOException {
        LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
        log.fine("I'm starting the http server");
        NHttpServer nhs=Http.createServer((req, resp)->{
            log.log(Level.FINE,"Http request incoming. Url:{0}",req.getRequestLine().getUri());
            resp.addHeader("Content-Type","text/plain");
            resp.end("Hello world!");
        });
        nhs.listen(80).whenComplete((ok, ex)->{
            if(ex!=null) {
                log.log(Level.SEVERE, "Error binding to port ",ex);
            }
        });
        nhs.onError(ex -> {
            log.log(Level.SEVERE, "Socket error ",ex);
        });
        JNodeCore.get().loop();
    }
}
