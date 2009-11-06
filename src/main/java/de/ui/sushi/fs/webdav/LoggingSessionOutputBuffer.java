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

package de.ui.sushi.fs.webdav;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

public class LoggingSessionOutputBuffer extends SocketOutputBuffer {
    private final Logger logger;

    public LoggingSessionOutputBuffer(Socket socket, int buffersize, HttpParams params, Logger logger) throws IOException {
    	super(socket, buffersize, params);
        this.logger = logger;
    }
    
    @Override
    public void write(byte[] bytes, int ofs, int len) throws IOException {
        super.write(bytes,  ofs,  len);
        log(bytes, ofs, len);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        super.write(bytes);
        log(bytes);
    }

    @Override
    public void write(int bytes) throws IOException {
        super.write(bytes);
        log((byte) bytes);
    }

    @Override
    public void writeLine(CharArrayBuffer buffer) throws IOException {
        super.writeLine(buffer);
        log(buffer.toString());
    }

    @Override
    public void writeLine(String str) throws IOException {
        super.writeLine(str);
        log(str + "[EOL]");
    }

    //--
    
    private void log(byte ... bytes) {
    	log(bytes, 0, bytes.length);
    }
    private void log(byte[] bytes, int ofs, int length) {
    	log(new String(bytes, ofs, length));
    }
    private void log(String str) {
    	logger.fine(">>> " + str);
    }
}
