/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.beezle.sushi.fs.webdav;

import net.sf.beezle.sushi.util.Strings;

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
