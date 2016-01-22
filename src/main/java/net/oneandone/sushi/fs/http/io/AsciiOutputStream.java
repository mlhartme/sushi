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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** OutputStream that can write Strings line - ascii-encoded and terminated with CRLF */
public class AsciiOutputStream extends BufferedOutputStream {
    private static final byte[] CRLF = new byte[] { 13, 10 };

    public AsciiOutputStream(OutputStream dest, int buffersize) {
        super(dest, buffersize);
    }

    public void writeAscii(char c) throws IOException {
        write(c); // ASCII conversion
    }

    public void writeAscii(CharSequence s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            writeAscii(s.charAt(i));
        }
    }

    public void writeAsciiLn(CharSequence s) throws IOException {
        writeAscii(s);
        writeAsciiLn();
    }

    public void writeAsciiLn() throws IOException {
        write(CRLF);
    }
}
