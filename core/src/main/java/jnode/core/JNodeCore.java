/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOException;
import jnode.net.NSocketServerHandler;
import jnode.net.OnCloseHandler;
import jnode.net.OnDataHandler;
import jnode.net.OnErrorHandler;
import jnode.net.onDrainHandler;

/**
 *
 * @author daniele
 */
public class JNodeCore {

    private static final Logger log = Logger.getLogger(JNodeCore.class.getName());
    private static JNodeCore sampleJNode;
    private final Selector selector;
    private boolean isRunning = true;
    private final LinkedList<Runnable> runnables=new LinkedList<>();
    static {

        try {
            sampleJNode = new JNodeCore();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }

    }

    public JNodeCore() throws IOException {
        selector = Selector.open();
    }

    public void halt() {
        isRunning = false;
        selector.wakeup();
    }
    public void schedule(Runnable runnable) {
        runnables.add(runnable);
        selector.wakeup();
    }
    public synchronized void loop() {
        while (isRunning) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey sk = it.next();
                    ChannelEvent ce = (ChannelEvent) sk.attachment();
                    ce.onEvent(sk);
                    it.remove();
                }
                Iterator<Runnable> it2=runnables.iterator();
                while(it2.hasNext()) {
                    try {
                        it2.next().run();
                    } catch(Throwable t) {
                        log.log(Level.SEVERE, "Exception not manager", t);
                    }
                    it2.remove();
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Fatal error", ex);
            }
        }
    }

    public static JNodeCore get() {
        if (sampleJNode == null) {
            throw new IllegalStateException("No jnode found");
        }
        return sampleJNode;
    }

    
    public SelectionKey register(SelectableChannel sc,int ops,ChannelEvent cEvent) throws IOException{
        if(cEvent==null) throw new IllegalArgumentException();
        if(sc.isRegistered()) throw new IllegalStateException("Can't register same channel");
        return sc.register(selector, ops,cEvent);
    }



    @FunctionalInterface
    public interface ChannelEvent {

        void onEvent(SelectionKey a);
    }

}
