/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.http;

import java.nio.file.Path;
import org.apache.http.message.BasicHttpRequest;

/**
 *
 * @author daniele
 */
public class StaticServe implements NHttpServerHandler {
    public StaticServe(Path directory) {

    } 

    @Override
    public void incomingRequest(BasicHttpRequest req, NHttpResponse res) {
     
    }
    
}
