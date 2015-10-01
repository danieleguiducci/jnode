/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.core.builder;

import org.jnode.core.JNode.NContext;
import org.jnode.net.NServerSocket;
import org.jnode.net.NSocketServerHandler;

/**
 *
 * @author daniele
 */
public final class NBuilder {
    private NContext context;
    public NBuilder(NContext context) {
        this.context=context;
    }
    public NServerSocket createSocketServer(NSocketServerHandler listener) {
        return new NServerSocket(context,listener);
    }
    

}
