/**
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
package net.sf.beezle.sushi.fs.webdav.methods;

import net.sf.beezle.sushi.fs.webdav.MovedException;
import net.sf.beezle.sushi.fs.webdav.StatusException;
import net.sf.beezle.sushi.fs.webdav.WebdavConnection;
import net.sf.beezle.sushi.fs.webdav.WebdavNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Delete extends Method<Void> {
    public Delete(WebdavNode resource) {
        super("DELETE", resource);
    }

    @Override
    public Void processResponse(WebdavConnection connection, HttpResponse response) throws IOException {
        switch (response.getStatusLine().getStatusCode()) {
        case HttpStatus.SC_NO_CONTENT:
        	return null;
        case HttpStatus.SC_MOVED_PERMANENTLY:
        	throw new MovedException();
        case HttpStatus.SC_NOT_FOUND:
        	throw new FileNotFoundException(getUri());
       	default:
        	throw new StatusException(response.getStatusLine());
        }
    }
}
