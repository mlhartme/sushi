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

package net.sf.beezle.sushi.io;

import java.io.IOException;
import java.io.OutputStream;

/** Filter stream, that skip the specified number of bytes */
public class SkipOutputStream extends OutputStream {
    private final OutputStream out;
    private int skip;

    public SkipOutputStream(OutputStream out, int skip) {
        this.out = out;
        this.skip = skip;
    }

    public void write(int b) throws IOException {
        if (skip > 0) {
            skip--;
        } else {
            out.write(b);
        }
    }

    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (skip > 0) {
            if (skip >= len) {
                skip -= len;
                return;
            }
            off += len;
            len -= skip;
            skip = 0;
        }
        out.write(b, off, len);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }
}
