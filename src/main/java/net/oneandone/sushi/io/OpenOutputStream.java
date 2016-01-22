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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** keeps the underlying stream open */
public class OpenOutputStream extends FilterOutputStream {
    private boolean closed;

    public OpenOutputStream(OutputStream out) {
        super(out);
        this.closed = false;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            out.flush();
        }
    }

    @Override
    public void write(byte[] b, int ofs, int len) throws IOException {
        if (closed) {
            throw new IOException("stream is already closed");
        }
        super.write(b, ofs, len);
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("stream is already closed");
        }
        super.write(b);
    }
}
