/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.LoggerFactory;

/**
 *
 * @author daniele
 */
public class ByteBufferPool {
    private final static org.slf4j.Logger log = LoggerFactory.getLogger(ByteBufferPool.class);
    private final int size;
    private final ConcurrentLinkedQueue<ByteBuffer> freeBb=new ConcurrentLinkedQueue<>();
    public ByteBufferPool(int size) {
        this.size=size;
    }
    public ByteBuffer request() {
        ByteBuffer bb=freeBb.poll();
        if(bb==null) 
            bb=ByteBuffer.allocateDirect(size);
        else
            bb.clear();
        return bb;
    }
    public void release(ByteBuffer bb) {
        if(bb.capacity()!=size && !bb.isDirect())
            throw new IllegalArgumentException("Not valid ByteBuffer buffer");
        freeBb.add(bb);
    }
}
