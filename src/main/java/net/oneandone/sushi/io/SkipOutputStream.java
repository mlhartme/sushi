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
package net.oneandone.sushi.io;

import java.io.IOException;
import java.io.OutputStream;

/** Filter stream, that skip the specified number of bytes */
public class SkipOutputStream extends OutputStream {
    private final OutputStream out;
    private long skip;
    private long count;

    public SkipOutputStream(OutputStream out, long skip) {
        this.out = out;
        this.skip = skip;
        this.count = 0;
    }

    /** @return bytes actually written */
    public long count() {
        return count;
    }

    public void write(int b) throws IOException {
        if (skip > 0) {
            skip--;
        } else {
            out.write(b);
            count++;
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
            off += skip;
            len -= skip;
            skip = 0;
        }
        out.write(b, off, len);
        count += len;
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void close() throws IOException {
        out.close();
    }
}
