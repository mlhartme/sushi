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
import java.io.InputStream;

public class LoggingAsciiInputStream extends InputStream {
    private final InputStream src;
    private final LineLogger logger;

    public LoggingAsciiInputStream(InputStream src, LineLogger logger) {
        this.src = src;
        this.logger = logger;
    }

    @Override
    public int read() throws IOException {
        int result;

        result = src.read();
        if (result != -1) {
            logger.log((byte) result);
        }
        return result;
    }

    @Override
    public int read(byte[] bytes, int ofs, int len) throws IOException {
        int result;

        result = src.read(bytes,  ofs,  len);
        if (result != -1) {
            logger.log(bytes, ofs, result);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        logger.flush();
        src.close();
    }
}
