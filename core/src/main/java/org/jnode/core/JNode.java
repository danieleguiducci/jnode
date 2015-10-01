/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jnode.core.builder.NBuilder;
import org.jnode.net.ByteBufferCache;

/**
 *
 * @author daniele
 */
public class JNode {

    public static final LoadBalancerStrategy BALANCER_LOADFACTOR
            = (Looper a, Looper b) -> {
                return Double.compare(a.getLoadFactor(), b.getLoadFactor());
            };

    private static JNode base;
    private final Looper[] loopers;
    private LoadBalancerStrategy loadBalancer = BALANCER_LOADFACTOR;
    private boolean isRunning = true;
    private final ByteBufferCache bbCache;
    private final NContext ncontext = new NContext();
    private final NBuilder nbuilder=new NBuilder(ncontext);
    private JNode(int threadsCount) {
        loopers = new Looper[threadsCount];
        bbCache = new ByteBufferCache(1 << 29);
    }

    public void setLoadBalancer(LoadBalancerStrategy loadBalancer) {
        if (loadBalancer == null)
            throw new NullPointerException();
        this.loadBalancer = loadBalancer;
    }
    public NBuilder getBuilder() {
        return nbuilder;
    }
    private void start() {
        try {
            for (int i = 0; i < loopers.length; i++) {
                loopers[i] = new Looper(this);
            }
            Arrays.stream(loopers).forEach((Looper e) -> e.start());
        } catch (IOException e) {
            throw new InitializationException(e);
        }
    }

    ;
    public synchronized void shutdown() {
        if (!isRunning)
            return;
        Arrays.stream(loopers).forEach((Looper e) -> e.shutdown());
        isRunning = false;
    }


    public List<Looper> getLoopers() {
        return Collections.unmodifiableList(Arrays.asList(loopers));
    }

    public synchronized static JNode get() {
        if (base == null) {
            base = create();
        }
        return base;
    }

    public static JNode create(int threadCount) {
        JNode jnode = new JNode(threadCount);
        jnode.start();
        return jnode;
    }

    public static JNode create() {
        return create(Runtime.getRuntime().availableProcessors());
    }

    @FunctionalInterface
    public interface LoadBalancerStrategy extends Comparator<Looper> {
    }

    public final class NContext {

        private NContext() {

        }

        public JNode getJNode() {
            return JNode.this;
        }
        public Looper requestLooper() {
            return Arrays.stream(loopers).sorted(loadBalancer).findFirst().get();
        }

        public ByteBufferCache getByteBufferCache() {
            return bbCache;
        }

    }
}
