/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author daniele
 */
class WriteOps {

    private ByteBuffer bb;
    private boolean toBeRelease;
    private int state = 0; // 0= filling buffer 1=from ByteBuffer to channel 2=WritingOperationComplete
    private ByteBufferCache bbcache;
    protected static WriteOps createFromBBCache(ByteBufferCache bbcache, int size) {
        WriteOps wo=new WriteOps();
        wo.toBeRelease=true;
        wo.state=0;
        wo.bb=bbcache.request(size);
        wo.bbcache=bbcache;
        return wo;
    }
    
   protected static WriteOps wrap(ByteBuffer bb) {
        WriteOps wo=new WriteOps();
        wo.toBeRelease=false;
        wo.state=1;
        wo.bb=bb;
        return wo;
    }
    protected int position() {
        return bb.position();
    }
    protected int remaining() {
        return bb.remaining();
    }
    protected boolean isReleasable() {
        return toBeRelease;
    }

    protected boolean hasRemaining() {
        return bb.hasRemaining();
    }
    protected void release() {
        if(state==2) return;
        state=2;
        if(this.toBeRelease) {
            this.bbcache.release(bb);
        }
    }
    protected int fillChannel(SocketChannel sock) throws IOException {
        if(state!=1) throw new IllegalStateException("Wrong state."+state);
        int c=sock.write(bb);
        if(!bb.hasRemaining()) 
            release();
        return c;
    }
    protected boolean isDone() {
        return state==2;
    }
    protected boolean isOpen() {
        return state==0;
    }
    protected void closeAndFlip() {
        if(state!=0) throw new IllegalStateException("Can't close the stream in the state "+state);
        state=1;
        bb.flip();
    }

    public void write(int b) {
        if (state != 0)
            throw new IllegalStateException("Stream is closed");
        bb.put((byte) b);
    }


    public void write(byte b[])  {
        if (state != 0)
            throw new IllegalStateException("Stream is closed");
        bb.put(b);
    }

 
    public void write(byte b[], int off, int len)  {
        if (state != 0)
            throw new IllegalStateException("Stream is closed");
        bb.put(b, off, len);
    }

}
