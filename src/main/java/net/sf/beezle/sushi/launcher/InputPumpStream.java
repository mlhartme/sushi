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

public class InputPumpStream extends Thread {
    private final InputStream in;
    private final OutputStream out;
    private Exception exception;
    private volatile boolean finishing;

    public InputPumpStream(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
        this.exception = null;
        this.finishing = false;
    }

    public void run() {
        try {
            while (true) {
                while (!finishing && in.available() == 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        exception = e;
                        return;
                    }
                }
                if (finishing) {
                    return;
                }
                out.write(in.read());
                out.flush();
            }
        } catch (IOException e) {
            exception = e;
        }
    }


    public void finish() throws IOException, InterruptedException {
        finishing = true;
        join();
        if (exception != null) {
            if (exception instanceof IOException) {
                throw (IOException) exception;
            } else {
                throw (InterruptedException) exception;
            }
        }
    }
}
