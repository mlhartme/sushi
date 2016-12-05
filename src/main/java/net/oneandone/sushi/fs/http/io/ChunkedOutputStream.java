/*
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
package net.oneandone.sushi.fs.http.io;

import java.io.IOException;
import java.io.OutputStream;

/** Write https://tools.ietf.org/html/rfc2616#section-3.6.1 streams */

public class ChunkedOutputStream extends OutputStream {
    private final AsciiOutputStream dest;

    private final byte[] buffer;
    /** index into buffer, always < buffer.length */
    private int pos;
    private boolean closed;

    public ChunkedOutputStream(AsciiOutputStream out) {
        this(2048, out);
    }

    public ChunkedOutputStream(int bufferSize, AsciiOutputStream out) {
        this(new byte[bufferSize], out);
    }

    public ChunkedOutputStream(byte[] buffer, AsciiOutputStream out) {
        this.buffer = buffer;
        this.dest = out;
        this.pos = 0;
        this.closed = false;
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("stream already closed");
        }
        buffer[pos] = (byte) b;
        pos++;
        if (pos == buffer.length) {
            flushBuffer();
        }
    }

    @Override
    public void write(byte[] src, int ofs, int len) throws IOException {
        if (closed) {
            throw new IOException("stream already closed");
        }
        if (pos + len < buffer.length) {
            System.arraycopy(src, ofs, buffer, pos, len);
            pos += len;
        } else {
            flushBuffer(src, ofs, len);
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void flush() throws IOException {
        flushBuffer();
        dest.flush();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            flushBuffer();
            dest.writeAsciiLn("0");
            dest.writeAsciiLn();
            dest.flush();
        }
    }

    //--

    private static final byte[] EMPTY = {};

    private void flushBuffer() throws IOException {
        flushBuffer(EMPTY, 0, 0);
    }

    /** flush buffer and bytes in append */
    private void flushBuffer(byte[] append, int ofs, int len) throws IOException {
        int count;

        count = pos + len;
        if (count > 0) {
            dest.writeAsciiLn(Integer.toHexString(count));
            dest.write(buffer, 0, pos);
            dest.write(append, ofs, len);
            dest.writeAsciiLn();
            pos = 0;
        }
    }
}
