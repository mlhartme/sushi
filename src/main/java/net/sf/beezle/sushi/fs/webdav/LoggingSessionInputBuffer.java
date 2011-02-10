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

package com.oneandone.sushi.fs.webdav;

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
      	logger.log(bytes, ofs, result);
        return result;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        int result;

        result = super.read(bytes);
        logger.log(bytes, 0, result);
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
