/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

/**
 *
 * @author daniele
 */
@FunctionalInterface
public interface NSocketServerHandler {

    public void incomingConnection(NSocket nsc);
    
}
