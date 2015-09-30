/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jnode.net;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author daniele
 */
public interface NOutput {
    void setEstimedFrameSize(int size);
    void write(int b);
    void write(byte b[]);
    void write(byte b[], int off, int len);
    void write(ByteBuffer bb);
    void flush();
}
