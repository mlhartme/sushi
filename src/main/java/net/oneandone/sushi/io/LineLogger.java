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
package net.oneandone.sushi.io;

import net.oneandone.sushi.util.Strings;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LineLogger {
    private final Logger logger;
    private final String prefix;
    private final int maxLineLength;
    private final StringBuilder line;

    public LineLogger(Logger logger, String prefix) {
        this(logger, prefix, prefix.length() + 1024);
    }

    public LineLogger(Logger logger, String prefix, int maxLineLength) {
        this.logger = logger;
        this.prefix = prefix;
        this.maxLineLength = maxLineLength;
        this.line = new StringBuilder(prefix);
    }

    public void log(byte b) {
        if (line.length() > maxLineLength) {
            flush();
        }
        switch (b) {
            case '\\':
                line.append("\\\\");
                break;
            case '\t':
                line.append("\\t");
                break;
            case '\r':
                line.append("\\r");
                break;
            case '\n':
                line.append("\\n");
                flush();
                break;
            default:
                if (b >= 32 && b < 128) {
                    line.append((char) b);
                } else {
                    line.append('\\').append(Strings.padLeft(Integer.toHexString(b & 0xff), 2, '0'));
                }
                break;
        }
    }

    public void log(byte[] bytes, int ofs, int length) {
        for (int i = ofs; i < ofs + length; i++) {
            log(bytes[i]);
        }
    }

    public void flush() {
        logger.log(Level.FINE, line.toString());
        line.setLength(prefix.length());
    }
}
