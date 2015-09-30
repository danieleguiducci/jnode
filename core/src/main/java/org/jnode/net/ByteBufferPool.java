/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author daniele
 */
class ByteBufferPool {
    private final int size;
    private final ConcurrentLinkedQueue<ByteBuffer> freeBb=new ConcurrentLinkedQueue<>();
    public ByteBufferPool(int size) {
        this.size=size;
    }
    protected ByteBuffer request() {
        ByteBuffer bb=freeBb.poll();
        if(bb==null) 
            bb=ByteBuffer.allocateDirect(size);
        return bb;
    }
    protected void release(ByteBuffer bb) {
        if(bb.capacity()!=size && !bb.isDirect())
            throw new IllegalArgumentException("Not valid ByteBuffer buffer");
        freeBb.add(bb);
    }
}
