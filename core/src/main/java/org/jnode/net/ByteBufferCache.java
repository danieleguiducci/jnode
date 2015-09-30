/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.nio.ByteBuffer;

/**
 *
 * @author daniele
 */
class ByteBufferCache {

    private final ByteBufferPool[] pools;
    private int maxSize;
    protected ByteBufferCache(int maxSize) {
        this.pools = new ByteBufferPool[32];
        if (maxSize <= 100)
            throw new IllegalArgumentException();
        this.maxSize=maxSize;
        int i = 0, size = 1;
        while (size <= maxSize) {
            pools[i] = new ByteBufferPool(size);
            i++;
            size = 1 << i;
        }
    }

    protected ByteBuffer request(int size) {
        if(size>=maxSize) throw new IllegalArgumentException("Size exceeding max size:"+maxSize+" asked:"+size);
        if (size <= 0)
            size = 128;
        return pools[log2(size)].request();
        
    }

    protected void release(ByteBuffer bb) {
        pools[log2(bb.capacity())].release(bb);
    }

    private static int log2(int x) {
        if (x <= 0)
            throw new IllegalArgumentException(x + " <= 0");
        int v = x, y = -1;
        while (v > 0) {
            v >>= 1;
            y++;
        }
        if ((1 << y) != x)
            y++;
        return y;
    }
}
