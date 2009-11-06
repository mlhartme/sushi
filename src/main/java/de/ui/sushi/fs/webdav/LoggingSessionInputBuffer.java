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

import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.params.HttpParams;
import org.apache.http.util.CharArrayBuffer;

public class LoggingSessionInputBuffer extends SocketInputBuffer {
    private final Logger logger;
    
    public LoggingSessionInputBuffer(Socket socket, int buffersize, HttpParams params, Logger logger) throws IOException {
    	super(socket, buffersize, params);
        this.logger = logger;
    }

    @Override
    public int read(byte[] bytes, int ofs, int len) throws IOException {
        int result;
        
        result = super.read(bytes,  ofs,  len);
      	log(bytes, ofs, result);
        return result;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        int result;
        
        result = super.read(bytes);
        log(bytes, 0, result);
        return result;
    }

    @Override
    public int read() throws IOException {
        int b;
        
        b = super.read();
        if (b != -1) { 
        	log((byte) b);
        }
        return b;
    }

    @Override
    public String readLine() throws IOException {
        String result;
        
        result = super.readLine();
        if (result != null) {
            log(result);
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
            log(new String(buffer.buffer(), pos, result));
        }
        return result;
    }

    //--

    private void log(byte ... bytes) {
    	log(bytes, 0, bytes.length);
    }
    private void log(byte[] bytes, int ofs, int length) {
    	log(new String(bytes, ofs, length));
    }
    private void log(String str) {
    	logger.fine("<<< " + str);
    }
}
