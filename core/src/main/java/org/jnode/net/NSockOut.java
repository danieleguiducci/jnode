/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 *
 * @author daniele
 */
public class NSockOut extends OutputStream implements NOutput{
    private ByteBufferCache bbcache;
    private int estimedFrameSize=4096;
    private final LinkedList<WriteOps> pendingWriteOps=new LinkedList<>();
    private WriteOps wo;
    protected NSockOut(ByteBufferCache bbCache) {
        wo=WriteOps.createFromBBCache(bbcache, estimedFrameSize);
    }
    
    @Override
    public void flush() {
        if(wo.position()==0) return;
        createWriteOps(estimedFrameSize);
    }
    private void createWriteOps(int size) {
        if(wo.position()==0)
            wo.release();
        else 
            pendingWriteOps.add(wo);
        wo=WriteOps.createFromBBCache(bbcache, estimedFrameSize);
    }
    @Override
    public void setEstimedFrameSize(int size) {
        if(size<=100) size=100;
        estimedFrameSize=size;
    }

    @Override
    public void write(ByteBuffer bb) {
        if(!bb.hasRemaining()) return;
        flush();
        pendingWriteOps.add(WriteOps.wrap(bb));
    }
    @Override
    public void write(int b) {
        if(wo.remaining()==0) createWriteOps(estimedFrameSize);
        wo.write(b);
    }
       @Override
    public void write(byte b[]) {
        if(wo.remaining()<b.length) createWriteOps(Math.max(b.length, estimedFrameSize));
        wo.write(b);
    }

    @Override
    public void write(byte b[], int off, int len)  {
        if(wo.remaining()<len) createWriteOps(Math.max(len, estimedFrameSize));
        wo.write(b, off, len);
    }
    /**
     * No effect on this implementation
     */
    @Override
    public void close() {
        
    }


}
