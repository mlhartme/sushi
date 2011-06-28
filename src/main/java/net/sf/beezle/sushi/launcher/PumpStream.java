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

package net.sf.beezle.sushi.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PumpStream extends Thread {
    private byte[] buffer;
    private final InputStream src;
    private final OutputStream dest;
    private IOException exception;

    public PumpStream(InputStream src, OutputStream dest) {
        this.buffer = new byte[1024];
        this.src = src;
        this.dest = dest;
        setDaemon(true);
    }

    public void run() {
        int len;

        try {
            while (true) {
                len = src.read(buffer);
                if (len == -1) {
                    dest.flush();
                    return;
                }
                dest.write(buffer, 0, len);
            }
        } catch (IOException e) {
            exception = e;
            return;
        }
    }

    public void finish(Launcher launcher) throws Failure {
        try {
            join();
        } catch (InterruptedException e) {
            throw new Interrupted(e);
        }
        if (exception != null) {
            throw new Failure(launcher, exception);
        }
    }
}
