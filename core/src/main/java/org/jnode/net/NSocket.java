/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import org.jnode.core.JNode;
import org.jnode.core.JNode.RegisterResult;
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
    private SelectionKey sk;
    private final JNode jnode;
    private Looper looper;
    private boolean isConnected = true;
    private static final Charset charset = Charset.forName("utf-8");
    public final NOutput out ;

    protected NSocket(JNode jnode) {
        this.jnode = jnode;
        out = new NSockOut();
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

    protected CompletableFuture<Void> setSocketChannel(SocketChannel sc) {
        this.sc = sc;
        return configure(sc).thenCompose((SocketChannel sock) -> jnode.register(sc, SelectionKey.OP_READ, new SocketChannelEvent()))
                .thenAccept((RegisterResult rr) -> {
                    sk = rr.sk;
                    looper = rr.assignedLooper;
                });

    }

    public void onError(OnErrorHandler handler) {
        this.errorHandler = handler;
    }

    public void onClose(OnCloseHandler handler) {
        this.closeHanlder = handler;
    }

    public void onData(OnDataHandler handler) {
        this.dataHandler = handler;
    }

    public void onDrain(onDrainHandler handler) {
        this.drainHandler = handler;
    }

    private void _close() {
        sendConnectionDown();
        try {
            sc.close();
        } catch (IOException ex) {
            looper.schedule(() -> {
                _onError(ex);
            });
        }
    }

    @Override
    public void close() {
        if (Thread.currentThread().getId() == looper.getId())
            _close();
        else
            looper.schedule(() -> {
                this._close();
            });
    }

    private void sendConnectionDown() {
        if (!isConnected)
            return;
        sk.cancel();
        isConnected = false;
        looper.schedule(() -> {
            _onClose();
        });
    }

    public int read(ByteBuffer bb) {
        if (!isConnected)
            return 0;
        if (!sc.isConnected()) {
            sendConnectionDown();
            return 0;
        }
        try {
            int readed = sc.read(bb);
            if (readed == -1) {
                sendConnectionDown();
                return 0;
            }
            return readed;
        } catch (IOException ex) {
            looper.schedule(() -> {
                _onError(ex);
            });
            return 0;
        }

    }


    private boolean processWriteOps() {
        if(readWriteOps.isEmpty()) return false;
        try {
            while( !readWriteOps.isEmpty()) {
                WriteOps wo=readWriteOps.getFirst();
                int count=wo.fillChannel(sc);
                if(wo.isDone()) {
                    wo.release();
                    readWriteOps.poll();
                }
                if(count==0)
                    break;
            }
            sk.interestOps(sk.interestOps() | SelectionKey.OP_WRITE);
            return true;
        } catch (IOException ex) {
            sendConnectionDown();
            return false;
        }
    }

    public void executeSafe(Runnable r) {
        if(Thread.currentThread().getId()!=looper.getId()) {
            looper.schedule(r);
            return;
        } 
        try {
            r.run();
        } catch(Throwable t) {
            log.error("Error. Uncatched exception ",t);
        }
        
    }
    private void _onClose() {
        if (closeHanlder == null)
            return;
        try {
            closeHanlder.onClose();
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
            if (!a.isValid()) {
                return;
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
                if(!processWriteOps()) {
                    if(sk.isValid() && isConnected) {
                        sk.interestOps(sk.interestOps() & (~SelectionKey.OP_WRITE));
                        _onDrain();
                    }
                }
            }
        }
    }
    private void checkThread() {
        if(Thread.currentThread().getId()!=looper.getId()) 
            throw new NotYourThreadException();
    }
    private final LinkedList<WriteOps> readWriteOps = new LinkedList<>();
    private class NSockOut extends  NOutput {

        private int estimedFrameSize = 4096;
        
        private WriteOps wo;

        protected NSockOut() {
            super();
            wo = WriteOps.createFromBBCache(jnode.getByteBufferCache(), estimedFrameSize);
        }
        
        @Override
        public void flush() {
            if (wo.position() == 0)
                return;
            checkThread();
            createWriteOps(estimedFrameSize);
        }
        private void appendWriteOps(WriteOps writeOps) {
            if(writeOps.isOpen())writeOps.closeAndFlip();
            readWriteOps.add(writeOps);
            processWriteOps();
        }
        private void createWriteOps(int size) {
            if (wo.position() == 0)
                wo.release();
            else
                appendWriteOps(wo);
            wo = WriteOps.createFromBBCache(jnode.getByteBufferCache(), estimedFrameSize);
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
            if(Thread.currentThread().getId()!=looper.getId()) {
                looper.schedule(()->{flush();});
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
        }
    }
}
