/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.http;

import org.apache.http.message.BasicHttpRequest;

/**
 *
 * @author daniele
 */
@FunctionalInterface
public interface NHttpServerHandler {
    public void incomingRequest(BasicHttpRequest req, NHttpResponse res);
}
