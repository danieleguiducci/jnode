/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.http;

import java.nio.charset.Charset;
import org.jnode.net.NSocket;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.jnode.core.Looper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author daniele
 */
public class NHttpResponse {
    private final static Logger log = LoggerFactory.getLogger(NHttpResponse.class);
    private final NSocket sock;
    private final BasicHttpResponse bhr;
    private boolean isHeaderSent=false;
    private static final Charset charset=Charset.forName("utf-8");
    protected NHttpResponse(NSocket sock) {
        this.sock = sock;
        bhr=new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_OK, "OK");
        bhr.addHeader("Transfer-Encoding","chunked");
        bhr.addHeader("Connection","close");
        sock.onDrain(()->{
            if(isDone)
                sock.close();
        });
        sock.out.setCharset(charset);
    }
    public void setCode(int code) {
        if(isHeaderSent) throw new IllegalStateException("Header already sent");
        bhr.setStatusLine(HttpVersion.HTTP_1_1, code);
    }
    public void addHeader(Header header) {
        if(isHeaderSent) throw new IllegalStateException("Header already sent");
        bhr.addHeader(header);
    }
    public void addHeader(String key,String value) {
        if(isHeaderSent) throw new IllegalStateException("Header already sent");
        bhr.setHeader(key, value);
    }
    private void sendHeader() {
        sock.out.println(bhr.getStatusLine().toString());
        HeaderIterator it = bhr.headerIterator();
        while (it.hasNext()) {
            sock.out.println(it.nextHeader().toString());
        }
        sock.out.println();
        isHeaderSent=true;
    }
    private void compose(String text) {
        byte[] data=text.getBytes(charset);
        sock.out.println(Integer.toHexString(data.length));
        sock.out.write(data);
        sock.out.println();
    }
    private void _write(String data) {

    }
    private boolean isDone=false;
    public void write(String data) {
        if(!isHeaderSent) {
            sendHeader();
            if(data.length()>0) compose(data);
        } else {
            if(data.length()==0) return;
            compose(data);
        }
    }
    public void end() {
        compose("");
        isDone=true;
        sock.out.flush();
    }
    public void end(String data) {
        write(data);
        compose("");
        isDone=true;
        sock.out.flush();
    }

}
