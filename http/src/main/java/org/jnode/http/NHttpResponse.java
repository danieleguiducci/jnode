/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.http;

import org.jnode.net.NSocket;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.jnode.core.JNodeCore;
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
    protected NHttpResponse(NSocket sock) {
        this.sock = sock;
        bhr=new BasicHttpResponse(HttpVersion.HTTP_1_1,HttpStatus.SC_OK, "OK");
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
    private StringBuilder sendHeader() {
        StringBuilder sb=new StringBuilder();
        sb.append(bhr.getStatusLine().toString()).append("\n");
        HeaderIterator it = bhr.headerIterator();
        while (it.hasNext()) {
            sb.append(it.nextHeader().toString()).append("\n");
        }
        sb.append("\n");
        isHeaderSent=true;
        return sb;
    }
    public void write(String data) {
        
        if(!isHeaderSent) {
            StringBuilder sb=sendHeader();
            sb.append(data);
            sock.write(sb.toString());
        } else {
            sock.write(data);
        }
        
    }
    public void end(String data) {
        write(data);
        end();
    }
    public void end() {
        if(sock.pendingData()) {
            
            sock.onDrain(()->{sock.close();});
        } else {
            sock.close();
        }
        
    }
}
