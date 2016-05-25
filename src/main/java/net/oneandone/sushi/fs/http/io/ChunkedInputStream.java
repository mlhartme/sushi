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
package net.oneandone.sushi.fs.http.io;

import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.fs.http.model.ProtocolException;
import net.oneandone.sushi.fs.http.model.Scanner;
import net.oneandone.sushi.io.Buffer;

import java.io.IOException;
import java.io.InputStream;

/** Read https://tools.ietf.org/html/rfc2616#section-3.6.1 streams */

public class ChunkedInputStream extends InputStream {
    private static final int UNKNOWN = -1;

    private final AsciiInputStream src;
    private final Buffer skipBuffer;

    /** length of current chunk, UNKNOWN if it's not yet read */
    private int length;
    private int pos;
    private boolean eof;
    private boolean closed;

    public ChunkedInputStream(AsciiInputStream src, Buffer skipBuffer) {
        this.src = src;
        this.length = UNKNOWN;
        this.pos = 0;
        this.eof = false;
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

        if (closed) {
            throw new IOException("stream already closed");
        }
        before();
        b = src.read();
        if (b != -1) {
            pos++;
            if (pos >= length) {
                afterData();
            }
        }
        return b;
    }

    @Override
    public int read(byte[] b, int ofs, int len) throws IOException {
        int bytesRead;

        if (closed) {
            throw new IOException("Attempted read from closed stream.");
        }
        before();
        bytesRead = src.read(b, ofs, Math.min(len, length - pos));
        if (bytesRead == -1) {
            eof = true;
            throw new ProtocolException("chunk truncated, expected " + length + ", code " + pos + " bytes");
        }
        pos += bytesRead;
        if (pos >= length) {
            afterData();
        }
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
            if (!eof) {
                skipBuffer.skip(this, Long.MAX_VALUE);
            }
        } finally {
            eof = true;
            closed = true;
        }
    }

    //--

    private void before() throws IOException {
        if (length == UNKNOWN) {
            length = readLength();
            pos = 0;
            if (length == 0) {
                eof = true;
                HeaderList.parse(src); // result is ignored
            }
        }
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

        str = src.readLine();
        if (str == null || !str.isEmpty()) {
            throw new ProtocolException("expected empty line, got " + str);
        }
        length = UNKNOWN;
    }
}
