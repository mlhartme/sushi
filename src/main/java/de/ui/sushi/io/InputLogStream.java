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

package de.ui.sushi.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FilterInputStream;

public class InputLogStream extends FilterInputStream {
	private final OutputStream log;

	public InputLogStream(InputStream src, OutputStream log) {
		super(src);
		this.log = log;
    }

	@Override
    public int read() throws IOException {
        int c;
        
        c = in.read();
        if (c != -1) {
            log.write((char) c);
            if (c == '\n') {
            	log.flush();
            }
        }
        return c;
    }
	
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int result;
        
        result = in.read(b, off, len);
        if (result != -1) {
            log.write(b, off, result);
            log.flush();
        }
        return result;
    }
}
