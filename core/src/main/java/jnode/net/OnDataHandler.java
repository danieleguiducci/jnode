/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jnode.net;

import java.nio.ByteBuffer;

/**
 *
 * @author daniele
 */
@FunctionalInterface
public interface OnDataHandler {
    void onDataIncoming(NSocket sock);
}