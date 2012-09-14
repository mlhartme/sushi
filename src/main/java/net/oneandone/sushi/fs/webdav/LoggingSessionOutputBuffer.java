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
package net.oneandone.sushi.fs.webdav;

import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class LoggingSessionOutputBuffer extends SocketOutputBuffer {
    private final LineLogger logger;

    public LoggingSessionOutputBuffer(Socket socket, int buffersize, HttpParams params, Logger logger) throws IOException {
    	super(socket, buffersize, params);
        this.logger = new LineLogger(logger, ">>> ");
    }

    @Override
    public void write(byte[] bytes, int ofs, int len) throws IOException {
        super.write(bytes,  ofs,  len);
        logger.log(bytes, ofs, len);
    }

    @Override
    public void writeLine(CharArrayBuffer buffer) throws IOException {
        // the buffer is *not* written via super.write ...
        logger.log(buffer.toString());
        super.writeLine(buffer);
        // CRLF is written via super.write, thus, we have proper logging
    }
}
