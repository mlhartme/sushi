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
package net.oneandone.sushi.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BytePumper extends Pumper {
    private byte[] buffer;
    private final InputStream src;
    private final OutputStream dest;
    private final boolean flushDest;
    private final boolean closeDest;

    public BytePumper(InputStream src, OutputStream dest, boolean flushDest, boolean closeDest) {
        this.buffer = new byte[1024];
        this.src = src;
        this.dest = dest;
        this.flushDest = flushDest;
        this.closeDest = closeDest;
    }

    @Override
    public void runUnchecked() throws IOException {
        int len;

        while (true) {
            len = src.read(buffer);
            if (len == -1) {
                if (closeDest) {
                    dest.close();
                } else {
                    dest.flush();
                }
                return;
            }
            dest.write(buffer, 0, len);
            if (flushDest) {
                dest.flush();
            }
        }
    }
}
