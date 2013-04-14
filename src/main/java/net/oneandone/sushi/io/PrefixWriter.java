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

import java.io.PrintWriter;
import java.io.Writer;

/**
 * A PrintWriter with a modifiable prefix and auto-flush.
 * Auto-flush is better than the underlying PrintWriter because every character is checked.
 */
public class PrefixWriter extends PrintWriter {
    private String prefix;
    private final String newline;
    private final int length;
    private int matched;

    public PrefixWriter(Writer out) {
        this(out, "", System.getProperty("line.separator"));
    }

    public PrefixWriter(Writer out, String prefix, String newline) {
        super(out, false);
        this.prefix = prefix;
        this.newline = newline;
        this.length = newline.length();
        this.matched = length;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Writer getOut() {
        return out;
    }

    public void setOut(Writer out) {
        this.out = out;
    }

    //--

    @Override
    public void write(int c) {
        if (matched == length) {
            super.write(prefix, 0, prefix.length());
            matched = 0;
        }
        super.write(c);
        if (c == newline.charAt(matched)) {
            matched++;
            if (matched == length) {
                flush();
            }
        } else {
            matched = 0;
        }
    }

    @Override
    public void write(char cbuf[], int off, int len) {
        char c;

        for (int i = 0; i < len; i++) {
            c = cbuf[i + off];
            write(c);
        }
    }

    @Override
    public void write(String str, int off, int len) {
        char c;

        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            write(c);
        }
    }

    public void println() {
        write(newline);
    }
}
