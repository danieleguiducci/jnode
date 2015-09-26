/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.core.net;

import jnode.core.Loop.NSocketChannel;

/**
 *
 * @author daniele
 */
@FunctionalInterface
public interface NSocketServerListener {

    public void incomingConnection(NSocketChannel nsc);
    
}
