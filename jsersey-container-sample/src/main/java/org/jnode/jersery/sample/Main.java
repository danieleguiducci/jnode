/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.jersery.sample;

import org.glassfish.jersey.server.ResourceConfig;
import org.jnode.jersey.JNContainer;

/**
 *
 * @author daniele
 */
public class Main {
    public static void main(String arg[]) {
        ResourceConfig res=new ResourceConfig();
        res.packages("org.jnode.jersery.sample");
        JNContainer con=new JNContainer(res);
        con.start();
    }
}
