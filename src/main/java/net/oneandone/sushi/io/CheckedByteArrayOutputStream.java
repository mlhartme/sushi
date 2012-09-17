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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CheckedByteArrayOutputStream extends ByteArrayOutputStream {
    private boolean closed;

    public CheckedByteArrayOutputStream() {
        closed = false;
    }

    /** @param add may be null */
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
