# jnode
```java
NServerSocketChannel nss = Loop.getLoop().createServer(socket -> {
    System.out.println("Incoming connection");
    socket.onData((ByteBuffer data) -> {
        String string = new String(data.array(), 0, data.remaining());
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
Loop.getLoop().loop();
```