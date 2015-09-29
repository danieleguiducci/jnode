/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.core;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    private JNode(int threadsCount) {
        loopers = new Looper[threadsCount];

    }

    public void setLoadBalancer(LoadBalancerStrategy loadBalancer) {
        if (loadBalancer == null)
            throw new NullPointerException();
        this.loadBalancer = loadBalancer;
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

    public static class RegisterResult {

        public Looper assignedLooper;
        public SelectionKey sk;

        public RegisterResult(Looper assignedLooper, SelectionKey sk) {
            this.assignedLooper = assignedLooper;
            this.sk = sk;
        }

    }
    public List<Looper> getLoopers() {
        return Collections.unmodifiableList(Arrays.asList(loopers));
    }
    public CompletableFuture<RegisterResult> register(SelectableChannel sc, int ops, Looper.ChannelEvent cEvent) {
        Arrays.sort(loopers, loadBalancer);
        Looper l = loopers[0];
        return l.register(sc, ops, cEvent).thenCompose((SelectionKey ret) -> {
            return CompletableFuture.completedFuture(new RegisterResult(l, ret));
        });
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

}
