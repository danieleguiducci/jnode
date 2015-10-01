# jnode
```java
NServerSocket nss = Net.createServer((NSocket socket) -> {
    log.trace("Incoming connection");
    socket.out.println("Welcome in the Echo Server");
    socket.out.flush();
    socket.onData(sock -> {
        // Allocate a new BB each read operation is bad. We are working on it
        ByteBuffer bb=ByteBuffer.allocate(200);
        sock.read(bb);
        bb.flip();
        socket.out.write(bb);
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
```


```java
NHttpServer nhs=Http.createServer((req, resp)->{
    log.trace("Http request incoming. Url:{0}",req.getRequestLine().getUri());
    resp.addHeader("Content-Type","text/html; charset=utf-8");
    resp.end("Hello world è!");

});
nhs.listen(80).whenComplete((ok, ex)->{
    if(ex!=null) {
        log.error("Error binding to port ",ex);
    }
});
nhs.onError(ex -> {
    log.error("Socket error ",ex);
});
```
