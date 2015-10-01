/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.Flushable;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 *
 * @author daniele
 */
public abstract class NOutput extends OutputStream implements Flushable{
    private Charset charset;
    public NOutput() {
        setCharset(Charset.forName("utf-8"));
    }
    
    public abstract void setEstimedFrameSize(int size);
    @Override
    public abstract void write(int b);
    @Override
    public abstract void write(byte b[]);
    @Override
    public abstract void write(byte b[], int off, int len);

 
    public abstract void write(ByteBuffer bb);
    @Override
    public void flush() {
    }
    
    
    private byte[] newLine;
    public final void setCharset(Charset charset) {
        this.charset=charset;
        newLine="\r\n".getBytes(charset);
    }

    public void print(String string) {
        if(string.length()==0) return;
        write(string.getBytes(charset));
    }
    public void println(String string) {
        if(string.length()==0) return;
        write(string.getBytes(charset));
        write(newLine);
    }
    public void println() {
        write(newLine);
    }
}
