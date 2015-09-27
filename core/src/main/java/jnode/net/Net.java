/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.net;

import jnode.core.JNodeCore;
import jnode.net.NServerSocket;
import jnode.net.NSocketServerHandler;

/**
 *
 * @author daniele
 */
public class Net {
    public static NServerSocket createServer(NSocketServerHandler listener) {
        return new NServerSocket(JNodeCore.get(), listener);
    }
    
}
