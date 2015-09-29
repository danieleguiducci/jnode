# jnode
```java
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
```


```java
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
```
