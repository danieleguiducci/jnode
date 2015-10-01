/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.core;

/**
 *
 * @author daniele
 */
public class NotYourThreadException extends RuntimeException{

    public NotYourThreadException() {
        this("Your trying to access to a socket from another thread. Use sock.executeSafe()");
    }

    public NotYourThreadException(String message) {
        super(message);
    }

    public NotYourThreadException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotYourThreadException(Throwable cause) {
        super(cause);
    }

    public NotYourThreadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    
    
}
