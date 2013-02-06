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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public class PrefixWriter extends FilterWriter {
    private final char newline;
    private final String prefix;
    private boolean start;

    public PrefixWriter(Writer dest, String prefix, char newline) {
        super(dest);
        this.prefix = prefix;
        this.newline = newline;
        this.start = true;
    }

    @Override
    public void write(int c) throws IOException {
        if (start) {
            out.write(prefix);
            start = false;
        }
        out.write(c);
        if (c == newline) {
            start = true;
        }
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
        char c;

        for (int i = 0; i < len; i++) {
            c = cbuf[i + off];
            write(c);
        }
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        char c;

        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            write(c);
        }
    }
}
