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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CheckedByteArrayOutputStream extends ByteArrayOutputStream {
    private boolean closed;

    public CheckedByteArrayOutputStream() {
        closed = false;
    }

    /** @add may be null */
    public CheckedByteArrayOutputStream(byte[] add) throws IOException {
        this();
        if (add != null) {
            write(add);
        }
    }

    @Override
    public void write(int c) {
        ensureOpen();
        super.write(c);
    }

    @Override
    public void write(byte b[], int off, int len) {
        ensureOpen();
        super.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }
    
    public void ensureOpen() {
        if (closed) {
            throw new IllegalStateException();
        }
    }
}
