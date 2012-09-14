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

import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

public class LoggingSessionInputBuffer extends SocketInputBuffer {
    private final LineLogger logger;

    public LoggingSessionInputBuffer(Socket socket, int buffersize, HttpParams params, Logger logger) throws IOException {
    	super(socket, buffersize, params);
        this.logger = new LineLogger(logger, "<<< ");
    }

    @Override
    public int read(byte[] bytes, int ofs, int len) throws IOException {
        int result;

        result = super.read(bytes,  ofs,  len);
        if (result != -1) {
      	    logger.log(bytes, ofs, result);
        }
        return result;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        int result;

        result = super.read(bytes);
        if (result != -1) {
            logger.log(bytes, 0, result);
        }
        return result;
    }

    @Override
    public int read() throws IOException {
        int b;

        b = super.read();
        if (b != -1) {
        	logger.log((byte) b);
        }
        return b;
    }

    @Override
    public String readLine() throws IOException {
        String result;

        result = super.readLine();
        if (result != null) {
            logger.log(result);
            logger.log("\r\n");
        }
        return result;
    }

    @Override
    public int readLine(CharArrayBuffer buffer) throws IOException {
        int result;
        int pos;

        result = super.readLine(buffer);
        if (result >= 0) {
            pos = buffer.length() - result;
            logger.log(new String(buffer.buffer(), pos, result));
            logger.log("\r\n");
        }
        return result;
    }
}
