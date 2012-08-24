/**
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
package net.sf.beezle.sushi.fs.ssh;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TimedOutputStream extends FilterOutputStream {
    private final long started;
    public long duration;

    public TimedOutputStream(OutputStream out) {
        super(out);
        this.started = System.currentTimeMillis();
        this.duration = 0;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        if (duration == 0) {
            duration = System.currentTimeMillis() - started;
        } else {
            // already closed
        }
    }
}
