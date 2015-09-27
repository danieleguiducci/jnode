/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.http;


/**
 *
 * @author daniele
 */
public class Http {
    public static NHttpServer createServer(NHttpServerHandler listener) {
        return new NHttpServer(listener);
    }
}
