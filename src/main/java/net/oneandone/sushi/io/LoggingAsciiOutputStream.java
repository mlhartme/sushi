/*
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

public class LoggingAsciiOutputStream extends OutputStream {
    private final OutputStream dest;
    private final LineLogger logger;

    public LoggingAsciiOutputStream(OutputStream dest, LineLogger logger) {
        this.dest = dest;
        this.logger = logger;
    }

    @Override
    public void write(int b) throws IOException {
        logger.log((byte) b);
        dest.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        logger.log(b, off, len);
        dest.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        logger.flush();
        dest.flush();
    }

    @Override
    public void close() throws IOException {
        logger.flush();
        dest.close();
    }
}
