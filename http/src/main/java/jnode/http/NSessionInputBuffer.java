/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.http;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import jnode.net.NSocket;
import org.apache.http.config.MessageConstraints;
import org.apache.http.impl.nio.reactor.SessionInputBufferImpl;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;
import org.apache.http.util.Args;

/**
 *
 * @author daniele
 */
public class NSessionInputBuffer extends SessionInputBufferImpl{

    public NSessionInputBuffer(int buffersize, int lineBuffersize, MessageConstraints constraints, CharsetDecoder chardecoder, ByteBufferAllocator allocator) {
        super(buffersize, lineBuffersize, constraints, chardecoder, allocator);
    }

    public NSessionInputBuffer(int buffersize, int lineBuffersize, CharsetDecoder chardecoder, ByteBufferAllocator allocator) {
        super(buffersize, lineBuffersize, chardecoder, allocator);
    }

    public NSessionInputBuffer(int buffersize, int lineBuffersize, ByteBufferAllocator allocator, HttpParams params) {
        super(buffersize, lineBuffersize, allocator, params);
    }

    public NSessionInputBuffer(int buffersize, int linebuffersize, HttpParams params) {
        super(buffersize, linebuffersize, params);
    }

    public NSessionInputBuffer(int buffersize, int lineBuffersize, Charset charset) {
        super(buffersize, lineBuffersize, charset);
    }

    public NSessionInputBuffer(int buffersize, int lineBuffersize, MessageConstraints constraints, Charset charset) {
        super(buffersize, lineBuffersize, constraints, charset);
    }

    public NSessionInputBuffer(int buffersize, int lineBuffersize) {
        super(buffersize, lineBuffersize);
    }

    public NSessionInputBuffer(int buffersize) {
        super(buffersize);
    }

    public int fill(final NSocket channel) {
        Args.notNull(channel, "Channel");
        setInputMode();
        if (!this.buffer.hasRemaining()) {
            expand();
        }
        return channel.read(this.buffer);
    }
}
