/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jnode.net.NSocket;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
/**
 *
 * @author daniele
 */
public class NHttpResponse {
    private static final Logger log = Logger.getLogger(NHttpResponse.class.getName());
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
    private void sendHeader() {
        if(isHeaderSent) return;
        StringBuilder sb=new StringBuilder();
        sb.append(bhr.getStatusLine().toString()).append("\n");
        HeaderIterator it = bhr.headerIterator();

        while (it.hasNext()) {
            sb.append(it.nextHeader().toString()).append("\n");
        }
        sb.append("\n");
        sock.write(sb.toString());
        isHeaderSent=true;
    }
    public void write(String data) {
        if(!isHeaderSent) sendHeader();
        sock.write(data);
    }
    public void end(String data) {
        write(data);
        end();
    }
    public void end() {
        sock.onDrain(()->{sock.close();});
    }
}
