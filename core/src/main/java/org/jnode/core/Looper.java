/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.core;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class Looper {

    private final static Logger log = LoggerFactory.getLogger(Looper.class);
    private final Selector selector;
    private boolean isRunning = true;
    private final LinkedList<Runnable> runnables = new LinkedList<>();
    private JNode jnode;
    private Thread thread;
    private long id;
    private double load=0;
    protected Looper(JNode jnode) throws IOException {
        selector = Selector.open();
        this.jnode=jnode;
    }
    public JNode getJNode() {
        return jnode;
    }
    protected void shutdown() {
        isRunning = false;
        selector.wakeup();
    }

    public void schedule(Runnable runnable) {
        runnables.add(runnable);
        selector.wakeup();
    }

    protected Thread start() {
        thread=new Thread(new Worker());
        thread.start();
        id=thread.getId();
        return thread;
    }
    public long getId() {
        return id;
    }
   
    public double getLoadFactor() {
        return load;
    }
    private class Worker implements Runnable {
        
        @Override
        public void run() {
            long cyclesTime=0,workTime=0;
            while (isRunning) {
                try {
                    long t0=System.nanoTime();
                    selector.select(700);
                    long t1=System.nanoTime();
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey sk = it.next();
                        ChannelEvent ce = (ChannelEvent) sk.attachment();
                        ce.onEvent(sk);
                        it.remove();
                    }

                    while (!runnables.isEmpty()) {
                        try {
                            runnables.poll().run();
                        } catch (Throwable t) {
                            log.error("uncatched exception in main loop", t);
                        }
                    }
                    long t2=System.nanoTime();
                    workTime+=t2-t1;
                    cyclesTime+=t2-t0;
                    if(cyclesTime>5e8) {// >half second
                        double loadOnCycle=workTime/((double)cyclesTime);
                        if(cyclesTime>10e9) { // >5 seconds
                            //log.debug(id+"] LOAD: >5 sec, uso il loadCycle: "+loadOnCycle+" workTime:"+workTime);
                            load=loadOnCycle;
                        } else {
                            double seconds=cyclesTime/(double)1e9;
                            double c=1.0/Math.pow(1.3, seconds);
                            load=load*c+loadOnCycle*(1.0-c);
                            //log.debug(id+"] LOAD: <5 loadCycle: "+loadOnCycle+" c:"+c+" newLoad:"+load+" WorkTime:"+workTime);
                        }
                        workTime=cyclesTime=0;
                    }
                    
                } catch (IOException ex) {
                    log.error("Fatal error", ex);
                    throw new IllegalStateException(ex);
                }
            }
        }

    }

    protected CompletableFuture<SelectionKey> register(SelectableChannel sc, int ops, ChannelEvent cEvent) {
        if (cEvent == null) throw new IllegalArgumentException();
        if (sc.isRegistered()) throw new IllegalStateException("Can't register same channel");

        CompletableFuture cf=new CompletableFuture();
        if(Thread.currentThread()==thread) {
            try {
                cf.complete(sc.register(selector, ops, cEvent));
            } catch (ClosedChannelException ex) {
                cf.completeExceptionally(ex);
            }
        } else {
            this.schedule(()->{
                try {
                    cf.complete(sc.register(selector, ops, cEvent));
                } catch (ClosedChannelException ex) {
                    cf.completeExceptionally(ex);
                }
            });
        }
        return cf;
    }

    @FunctionalInterface
    public interface ChannelEvent {

        void onEvent(SelectionKey a);
    }
    public class LooperKey {
        private final SelectionKey sk;
        private LooperKey(SelectionKey sk) {
            this.sk=sk;
        }

        public SelectableChannel channel() {
            return sk.channel();
        }

        public Selector selector() {
            return sk.selector();
        }

        public boolean isValid() {
            return sk.isValid();
        }

        public void cancel() {
            sk.cancel();
        }

        public int interestOps() {
            return sk.interestOps();
        }

        public SelectionKey interestOps(int ops) {
            return sk.interestOps(ops);
        }

        public int readyOps() {
            return sk.readyOps();
        }

        public final boolean isReadable() {
            return sk.isReadable();
        }

        public final boolean isWritable() {
            return sk.isWritable();
        }

        public final boolean isConnectable() {
            return sk.isConnectable();
        }

        public final boolean isAcceptable() {
            return sk.isAcceptable();
        }

        public final Object attach(Object ob) {
            return sk.attach(ob);
        }

        public final Object attachment() {
            return sk.attachment();
        }
        
    }
}
