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

import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.fs.http.model.ProtocolException;
import net.oneandone.sushi.fs.http.model.Scanner;
import net.oneandone.sushi.io.Buffer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Read https://tools.ietf.org/html/rfc2616#section-3.6.1 streams
 * Does not read into the underlying stream once the EOF marker was seen.
 */
public class ChunkedInputStream extends InputStream {
    private static final int UNKNOWN = -1;
    private static final int EOF = 0;

    private final AsciiInputStream src;
    private final Buffer skipBuffer;

    /** length of current chunk, UNKNOWN if it's not yet read, EOF if we've seen then end marker */
    private int length;
    private int pos;
    private boolean closed;

    public ChunkedInputStream(AsciiInputStream src, Buffer skipBuffer) {
        this.src = src;
        this.length = UNKNOWN;
        this.pos = 0;
        this.closed = false;
        this.skipBuffer = skipBuffer;
    }

    @Override
    public int available() throws IOException {
        return length == UNKNOWN ? 0 : Math.min(src.available(), length - pos);
    }

    @Override
    public int read() throws IOException {
        int b;

        if (!before()) {
            return -1;
        }
        b = src.read();
        if (b != -1) {
            pos++;
            afterData();
        }
        return b;
    }

    @Override
    public int read(byte[] b, int ofs, int len) throws IOException {
        int bytesRead;

        if (!before()) {
            return -1;
        }
        bytesRead = src.read(b, ofs, Math.min(len, length - pos));
        if (bytesRead == -1) {
            length = EOF;
            throw new ProtocolException("chunk truncated, expected " + length + ", code " + pos + " bytes");
        }
        pos += bytesRead;
        afterData();
        return bytesRead;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            if (length != EOF) {
                length = EOF;
                skipBuffer.skip(this, Long.MAX_VALUE);
            }
        } finally {
            closed = true;
        }
    }

    //--

    /**
     * Standard processing before reading data.
     *
     * @return false for eof
     */
    private boolean before() throws IOException {
        if (closed) {
            throw new IOException("Attempted read from closed stream.");
        }
        if (length == UNKNOWN) {
            length = readLength();
            pos = 0;
            if (length == EOF) {
                HeaderList.parse(src); // result is ignored
                return false;
            }
        }
        return true;
    }

    private int readLength() throws IOException {
        String size;
        String str;
        int idx;

        str = src.readLine();
        if (str == null) {
            throw new ProtocolException("closing chunk expected");
        }
        idx = str.indexOf(';');
        size = Scanner.substringTrimmed(str, 0, idx < 0 ? str.length() : idx);
        try {
            return Integer.parseInt(size, 16);
        } catch (NumberFormatException e) {
            throw new ProtocolException("invalid chunk size: " + size, e);
        }
    }

    private void afterData() throws IOException {
        String str;

        if (pos < length) {
            // nothing to do
        } else {
            if (pos > length) {
                throw new IllegalStateException(pos + " >" + length);
            }
            str = src.readLine();
            if (str == null || !str.isEmpty()) {
                throw new ProtocolException("expected empty line, got " + str);
            }
            length = UNKNOWN;
        }
    }
}
