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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/** InputStream that can read CRLF-terminated lines into (ascii-decoded) Strings. */
public class AsciiInputStream extends BufferedInputStream {
    private final char[] charBuffer;

    public AsciiInputStream(InputStream src, int buffersize) {
        super(src, buffersize);
        charBuffer = new char[buffersize];
    }

    /** @return null for eof */
    public String readLine() throws IOException {
        String buffered;
        StringBuilder builder;

        buffered = readBufferedLine();
        if (buffered != null) {
            return buffered;
        }
        builder = new StringBuilder(16);
        if (!readStreamLine(builder)) {
            return null;
        }
        return builder.toString();
    }

    private static final String EMPTY = "";

    private String readBufferedLine() throws IOException {
        int c;
        boolean prevCr;
        String result;
        int length;

        prevCr = false;
        for (int i = this.pos; i < this.count; i++) {
            c = buf[i];
            switch (c) {
                case '\n':
                    if (prevCr) {
                        length = (i - 1) - pos;
                    } else {
                        // in theory, this is an error, but the spec mandates to tolerate a single \n as line termination
                        length = i - pos;
                    }
                    result = length == 0 ? EMPTY : new String(charBuffer, pos, length);
                    pos = i + 1;
                    return result;
                case '\r':
                    prevCr = true;
                    break;
                default:
                    prevCr = false;
            }
            charBuffer[i] = (char) buf[i];
        }
        return null;
    }

    private boolean readStreamLine(StringBuilder builder) throws IOException {
        int c;
        boolean empty;
        boolean withCr;

        empty = true;
        withCr = false;
        while (true) {
            c = read();
            switch (c) {
                case -1:
                    if (empty) {
                        return false;
                    } else {
                        throw new IOException("truncated");
                    }
                case '\n':
                    if (withCr) {
                        builder.setLength(builder.length() - 1);
                    }
                    return true;
                case '\r':
                    withCr = true;
                    empty = false;
                    builder.append((char) c); // ascii conversion
                    break;
                default:
                    withCr = false;
                    empty = false;
                    builder.append((char) c); // ascii conversion
            }
        }
    }
}
