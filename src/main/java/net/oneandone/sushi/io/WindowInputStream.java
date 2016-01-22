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

import java.io.IOException;
import java.io.InputStream;

/** Wraps an input stream to read exactly the specified number of bytes. The wrapped stream is never closed. */
public class WindowInputStream extends InputStream {
    private static final int BUFFER_SIZE = 2048;

    private final long length;
    private long pos;
    private boolean closed;
    private InputStream in;

    public WindowInputStream(InputStream in, long length) {
        this.in = in;
        this.pos = 0;
        this.closed = false;
        this.length = length;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            try {
                if (pos < length) {
                    skip(length - pos);
                }
            } finally {
                closed = true;
            }
        }
    }

    @Override
    public int available() throws IOException {
        return Math.min(in.available(), (int) Math.min(length - pos, Integer.MAX_VALUE));
    }

    @Override
    public int read() throws IOException {
        int b;

        if (closed) {
            throw new IOException("stream is already closed");
        }

        if (pos >= length) {
            return -1;
        }
        b = in.read();
        if (b == -1) {
            if (pos < length) {
                throw new IOException("premature end of stream, " + length + " vs " + pos);
            }
        } else {
            pos++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int ofs, int len) throws java.io.IOException {
        int chunk;
        int count;

        if (closed) {
            throw new IOException("stream is already closed");
        }

        if (pos >= length) {
            return -1;
        }

        chunk = len;
        if (pos + len > length) {
            chunk = (int) (length - pos);
        }
        count = in.read(b, ofs, chunk);
        if (count == -1 && pos < length) {
            throw new IOException("premature end of stream, " + length + " vs " + pos);
        }
        if (count > 0) {
            pos += count;
        }
        return count;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        byte[] buffer;
        long remaining;
        long count;
        int chunk;

        if (n <= 0) {
            return 0;
        }
        buffer = new byte[BUFFER_SIZE];
        remaining = Math.min(n, length - pos);
        count = 0;
        while (remaining > 0) {
            chunk = read(buffer, 0, (int) Math.min(BUFFER_SIZE, remaining));
            if (chunk == -1) {
                break;
            }
            count += chunk;
            remaining -= chunk;
        }
        return count;
    }
}
