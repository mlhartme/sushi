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

package com.oneandone.sushi.fs.webdav.methods;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import com.oneandone.sushi.fs.webdav.StatusException;
import com.oneandone.sushi.fs.webdav.WebdavConnection;
import com.oneandone.sushi.fs.webdav.WebdavRoot;

public class PutMethod extends WebdavMethod<Void> {
    public PutMethod(WebdavRoot root, String path) {
        super(root, "PUT", path);
    }

    @Override
    protected void setContentHeader() {
        setRequestHeader(HTTP.TRANSFER_ENCODING, HTTP.CHUNK_CODING);
    }
    
    @Override
    public Void processResponse(WebdavConnection connection, HttpResponse response) throws IOException {
    	int status;
    	
    	status = response.getStatusLine().getStatusCode();
        if (status != HttpStatus.SC_NO_CONTENT && status != HttpStatus.SC_CREATED) {
        	throw new StatusException(response.getStatusLine());
        }
        return null;
    }
}