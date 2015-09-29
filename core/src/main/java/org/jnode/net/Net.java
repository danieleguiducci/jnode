/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import org.jnode.core.JNode;

/**
 *
 * @author daniele
 */
public class Net {
    public static NServerSocket createServer(NSocketServerHandler listener) {
        return new NServerSocket(JNode.get(), listener);
    }
    
}
