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
package net.oneandone.sushi.fs.webdav;

import net.oneandone.sushi.util.Strings;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LineLogger {
    private final Logger logger;
    private final String prefix;
    private final StringBuilder line;

    public LineLogger(Logger logger, String prefix) {
        this.logger = logger;
        this.prefix = prefix;
        this.line = new StringBuilder(prefix);
    }

    public void log(byte ... bytes) {
    	log(bytes, 0, bytes.length);
    }

    public void log(byte[] bytes, int ofs, int length) {
    	log(new String(bytes, ofs, length));
    }

    public void log(String str) {
        int prev;
        int idx;

        prev = 0;
        while (true) {
            idx = str.indexOf('\n', prev);
            if (idx == -1) {
                line.append(str.substring(prev, str.length()));
                return;
            }
            idx++;
            line.append(str.substring(prev, idx));
            logger.log(Level.FINE, Strings.escape(line.toString()));
            line.setLength(prefix.length());
            prev = idx;
        }
    }
}
