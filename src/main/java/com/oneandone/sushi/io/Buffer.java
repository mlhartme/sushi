/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oneandone.sushi.io;

import com.oneandone.sushi.fs.Node;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;

/**
 * <p>Buffer to access streams. </p>
 */
public class Buffer {
    public static final int DEFAULT_SIZE = 8192;

    private final byte[] buffer;

    /** Create a Buffer with UTF-8 encoding */
    public Buffer() {
        this(DEFAULT_SIZE);
    }

    public Buffer(int bufferSize) {
        this.buffer = new byte[bufferSize];
    }

    public Buffer(Buffer orig) {
        this(orig.buffer.length);
    }

    //--
    
    public int size() {
        return buffer.length;
    }
    
    public int fill(InputStream in) throws IOException {
        return fill(in, buffer, 0, buffer.length, null);
    }

    public int fill(InputStream in, boolean[] eof) throws IOException {
        return fill(in, buffer, 0, buffer.length, eof);
    }

    public static int fill(InputStream in, byte[] buffer) throws IOException {
        return fill(in, buffer, 0, buffer.length, null);
    }

    public static int fill(InputStream in, byte[] buffer, int start, int max, boolean[] eof) throws IOException {
        int chunk;
        int ofs;
        
        for (ofs = 0; ofs < max; ofs += chunk) {
            chunk = in.read(buffer, ofs, max - ofs);
            if (chunk < 0) {
                if (eof != null) {
                    eof[0] = true;
                }
                return ofs;
            }
        }
        return ofs;
    }

    public boolean diff(Buffer cmp, int length) {
        for (int i = 0; i < length; i++) {
            if (buffer[i] != cmp.buffer[i]) {
                return true;
            }
        }
        return false;
    }

    //--
    
    public void digest(InputStream src, MessageDigest digest) throws IOException {
        int numRead;
        
        while (true) {
            numRead = src.read(buffer);
            if (numRead < 0) {
                break;
            }
            digest.update(buffer, 0, numRead);
        }
    }

    //--

    public byte[] readBytes(InputStream src) throws IOException {
        ByteArrayOutputStream dest;
        
        dest = new ByteArrayOutputStream();
        copy(src, dest);
        return dest.toByteArray();
    }

    public String readLine(InputStream src, String encoding) throws IOException {
        ByteArrayOutputStream tmp;
        int c;
        
        tmp = new ByteArrayOutputStream();
        while (true) {
            c = src.read();
            if (c < 0) {
                if (tmp.size() > 0) {
                    throw new EOFException();
                }
                return null;
            } else if (c == '\n') {
                return tmp.toString(encoding);
            } else {
                tmp.write(c);
            }
        }
    }
    
    public String readString(InputStream src, String encoding) throws IOException {
        byte[] bytes;
        
        bytes = readBytes(src);
        return new String(bytes, encoding);
    }
    
    /** 
     * Copies all bytes.
     * 
     * @return number of bytes actually copied
     */
    public int copy(InputStream in, Node dest) throws IOException {
        OutputStream out;
        int result;
        
        out = dest.createOutputStream();
        result = copy(in, out);
        out.close();
        return result;
    }
    
    /** 
     * Copies all bytes.
     * 
     * @return number of bytes actually copied
     */
    public int copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, Integer.MAX_VALUE);
    }
    
    /**
     * Copies up to max bytes.
     * 
     * @return number of bytes actually copied
     */
    public int copy(InputStream in, OutputStream out, int max) throws IOException {
        int chunk;
        int all;
        int remaining;
        
        remaining = max;
        all = 0;
        while (remaining > 0) {
            chunk = in.read(buffer, 0, Math.min(remaining, buffer.length));
            if (chunk < 0) {
                break;
            }
            out.write(buffer, 0, chunk);
            all += chunk;
            remaining -= chunk;
        }
        out.flush();
        return all;
    }
}
