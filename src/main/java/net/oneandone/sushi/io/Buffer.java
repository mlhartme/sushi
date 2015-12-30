/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.sushi.io;

import net.oneandone.sushi.fs.Node;

import java.io.ByteArrayOutputStream;
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

    /** read until the buffer is full or the stream is eof */
    public int fill(InputStream in) throws IOException {
        int max;
        int chunk;
        int ofs;

        max = buffer.length;
        for (ofs = 0; ofs < max; ofs += chunk) {
            chunk = in.read(buffer, ofs, max - ofs);
            if (chunk < 0) {
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
        int count;
        byte[] result;
        ByteArrayOutputStream dest;

        // fill buffer first, try to handle without ByteArrayOutputStream
        count = fill(src);
        if (count < size()) {
            result = new byte[count];
            System.arraycopy(buffer, 0, result, 0, count);
            return result;
        }
        dest = new ByteArrayOutputStream(buffer.length * 2);
        dest.write(buffer, 0, count);
        copy(src, dest);
        return dest.toByteArray();
    }

    public String readString(InputStream src, String encoding) throws IOException {
        byte[] bytes;

        bytes = readBytes(src);
        return new String(bytes, encoding);
    }

    //--

    /** 
     * Copies all bytes.
     * 
     * @return number of bytes actually copied
     */
    public long copy(InputStream in, Node dest) throws IOException {
        long result;

        try (OutputStream out = dest.newOutputStream()) {
            result = copy(in, out);
        }
        return result;
    }
    
    /** 
     * Copies all bytes.
     * 
     * @return number of bytes actually copied
     */
    public long copy(InputStream in, OutputStream out) throws IOException {
        return copy(in, out, Integer.MAX_VALUE);
    }
    
    /**
     * Copies up to max bytes.
     * 
     * @return number of bytes actually copied
     */
    public long copy(InputStream in, OutputStream out, long max) throws IOException {
        int chunk;
        long all;
        long remaining;
        
        remaining = max;
        all = 0;
        while (remaining > 0) {
            // cast is save because the buffer.length is an integer
            chunk = in.read(buffer, 0, (int) Math.min(remaining, buffer.length));
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
