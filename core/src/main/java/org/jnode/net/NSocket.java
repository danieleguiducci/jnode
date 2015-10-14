/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.jnode.core.JNode.NContext;
import org.jnode.core.Looper;
import org.jnode.core.NotYourThreadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class NSocket implements Closeable {

    private final static Logger log = LoggerFactory.getLogger(Looper.class);
    private SocketChannel sc;
    private OnErrorHandler errorHandler;
    private OnDataHandler dataHandler;
    private onDrainHandler drainHandler;
    private OnCloseHandler closeHanlder;
    private OnConnectionEstablishedHandler enstablishedHandler;
    private SelectionKey sk;
    private final NContext ncontext;
    private final Looper looper;
    private int state = 0;
    private static final Charset charset = Charset.forName("utf-8");
    public final NOutput out;

    public NSocket(NContext ncontext) {
        this.ncontext = ncontext;
        out = new NSockOut();
        looper = ncontext.requestLooper();
    }

    private CompletableFuture<SocketChannel> configure(SocketChannel sc) {
        CompletableFuture<SocketChannel> cf = new CompletableFuture<>();
        try {
            sc.configureBlocking(false);
            sc.setOption(StandardSocketOptions.TCP_NODELAY, true);
            cf.complete(sc);
        } catch (Exception e) {
            cf.completeExceptionally(e);
        }
        return cf;
    }

    public CompletableFuture<NSocket> connect(InetSocketAddress dest) {
        try {

            sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.setOption(StandardSocketOptions.TCP_NODELAY, true);
            sc.connect(dest);
            return looper.register(sc, SelectionKey.OP_CONNECT, new SocketChannelEvent())
                    .thenCompose((SelectionKey rr) -> {
                        sk = rr;
                        state = 1;
                        return CompletableFuture.completedFuture(NSocket.this);
                    });
        } catch (Throwable e) {
            CompletableFuture cf = new CompletableFuture();
            cf.completeExceptionally(e);
            return cf;
        }
    }

    public CompletableFuture<Void> setSocketChannel(ServerSocketChannel ssc) {
        try {
            SocketChannel sc = ssc.accept();
            if (sc == null)
                throw new NullPointerException();
            return setSocketChannel(sc, SelectionKey.OP_READ);
        } catch (Throwable e) {
            CompletableFuture cf = new CompletableFuture();
            cf.completeExceptionally(e);
            return cf;
        }
    }

    private CompletableFuture<Void> setSocketChannel(SocketChannel sc, int op) {
        this.sc = sc;
        return configure(sc).thenCompose((SocketChannel sock) -> looper.register(sc, op, new SocketChannelEvent()))
                .thenAccept((SelectionKey rr) -> {
                    sk = rr;
                    state = 1;
                });

    }

    public NSocket onConnectionEstablished(OnConnectionEstablishedHandler handler) {
        this.enstablishedHandler = handler;
        return this;
    }

    public NSocket onError(OnErrorHandler handler) {
        this.errorHandler = handler;
        return this;
    }

    public NSocket onClose(OnCloseHandler handler) {
        this.closeHanlder = handler;
        return this;
    }

    public NSocket onData(OnDataHandler handler) {
        this.dataHandler = handler;
        return this;
    }

    public NSocket onDrain(onDrainHandler handler) {
        this.drainHandler = handler;
        return this;
    }

    @Override
    public void close() {
        checkThread();

        sendConnectionDown(DOWN_REASON.LOCAL_CLOSE);
        try {
            sc.close();
        } catch (IOException ex) {
            looper.schedule(() -> {
                _onError(ex);
            });
        }
    }

    public static enum DOWN_REASON {

        IO_EXCEPTION, REMOTE_CLOSE, LOCAL_CLOSE
    };
    private DOWN_REASON downReason = null;

    public DOWN_REASON getCloseReason() {
        return downReason;
    }

    private void sendConnectionDown(DOWN_REASON reason) {
        if (state == 2)
            return;
        sk.cancel();
        state = 2;
        downReason = reason;
        looper.schedule(() -> {
            _onClose();
        });
    }

    public int read(ByteBuffer bb) {
        if (state != 1)
            return -1;
        if (!sc.isConnected()) {
            sendConnectionDown(DOWN_REASON.LOCAL_CLOSE);
            return -1;
        }
        try {
            int readed = sc.read(bb);
            if (readed == -1) {
                sendConnectionDown(DOWN_REASON.REMOTE_CLOSE);
                return -1;
            }
            return readed;
        } catch (IOException ex) {
            looper.schedule(() -> {
                _onError(ex);
            });
            return -1;
        }

    }

    private boolean processWriteOps() {
        if (readWriteOps.isEmpty())
            return false;
        try {
            while (!readWriteOps.isEmpty()) {
                WriteOps wo = readWriteOps.getFirst();
                int count = wo.fillChannel(sc);
                if (wo.isDone()) {
                    wo.release();
                    readWriteOps.poll();
                }
                if (count == 0)
                    break;
            }
            sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
            return true;
        } catch (Throwable ex) { //CancelledKeyException
            sendConnectionDown(DOWN_REASON.IO_EXCEPTION);
            return false;
        }
    }

    public void executeSafe(Runnable r) {
        if (Thread.currentThread().getId() != looper.getId()) {
            looper.schedule(r);
            return;
        }
        try {
            r.run();
        } catch (Throwable t) {
            log.error("Error. Uncatched exception ", t);
        }

    }

    private void _onEnstablished() {
        if (enstablishedHandler == null)
            return;
        try {
            enstablishedHandler.onConnectionEstablished(this);
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }

    private void _onClose() {
        if (closeHanlder == null)
            return;
        try {
            closeHanlder.onClose(this);
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }

    private void _onError(Exception bb) {
        if (errorHandler == null) {
            return;
        }
        try {
            errorHandler.onError(bb);
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }

    private void _onData() {
        if (dataHandler == null) {
            return;
        }
        try {
            dataHandler.onDataIncoming(this);
        } catch (Throwable t) {
            log.error("Uncatched error", t);
        }
    }

    private void _onDrain() {
        if (drainHandler == null) {
            return;
        }
        try {
            drainHandler.onDrain();
        } catch (Throwable t) {
            log.error("Uncatched error", t);

        }
    }

    /**
     *
     * @author daniele
     */
    private class SocketChannelEvent implements Looper.ChannelEvent {

        @Override
        public void onEvent(SelectionKey a) {
            if (!a.isValid())
                return;

            if (a.isConnectable()) {
                try {
                    sc.finishConnect();
                    sk.interestOps(SelectionKey.OP_READ);
                    _onEnstablished();
                } catch (IOException ex) {
                    _onError(ex);
                    sendConnectionDown(DOWN_REASON.IO_EXCEPTION);
                }

            }
            if (a.isReadable()) {
                try {

                    if (!sc.isConnected()) {
                        _onClose();
                        a.cancel();
                    } else {
                        _onData();
                    }
                } catch (Exception e) {
                    _onError(e);
                }
            }
            if (a.isValid() && a.isWritable()) {
                if (!processWriteOps()) {
                    if (sk.isValid() && state == 1) {
                        sk.interestOps(sk.interestOps() & (~SelectionKey.OP_WRITE));
                        _onDrain();
                    }
                }
            }
        }
    }

    private void checkThread() {
        if (Thread.currentThread().getId() != looper.getId())
            throw new NotYourThreadException();
    }
    private final LinkedList<WriteOps> readWriteOps = new LinkedList<>();

    private class NSockOut extends NOutput {

        private int estimedFrameSize = 4096;

        private WriteOps wo;

        protected NSockOut() {
            super();
            wo = WriteOps.createFromBBCache(ncontext.getByteBufferCache(), estimedFrameSize);
        }

        @Override
        public void flush() {
            if (wo.position() == 0)
                return;
            checkThread();
            createWriteOps(estimedFrameSize);
        }

        private void appendWriteOps(WriteOps writeOps) {
            if (writeOps.isOpen())
                writeOps.closeAndFlip();
            readWriteOps.add(writeOps);
            processWriteOps();
        }

        private void createWriteOps(int size) {
            if (wo.position() == 0)
                wo.release();
            else
                appendWriteOps(wo);
            wo = WriteOps.createFromBBCache(ncontext.getByteBufferCache(), estimedFrameSize);
        }

        @Override
        public void setEstimedFrameSize(int size) {
            checkThread();
            if (size <= 100)
                size = 100;
            estimedFrameSize = size;
        }

        @Override
        public void write(ByteBuffer bb) {
            checkThread();
            if (Thread.currentThread().getId() != looper.getId()) {
                looper.schedule(() -> {
                    flush();
                });
                return;
            }
            if (!bb.hasRemaining())
                return;
            flush();
            appendWriteOps(WriteOps.wrap(bb));
        }

        @Override
        public void write(int b) {
            checkThread();
            if (wo.remaining() == 0)
                createWriteOps(estimedFrameSize);
            wo.write(b);
        }

        @Override
        public void write(byte[] b) {
            checkThread();
            if (wo.remaining() < b.length)
                createWriteOps(Math.max(b.length, estimedFrameSize));
            wo.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            checkThread();
            if (wo.remaining() < len)
                createWriteOps(Math.max(len, estimedFrameSize));
            wo.write(b, off, len);
        }

        /**
         * No effect on this implementation
         */
        @Override
        public void close() {
            NSocket.this.close();
        }
    }
}
