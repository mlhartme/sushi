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

package de.ui.sushi.fs.webdav.methods;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import de.ui.sushi.fs.webdav.StatusException;
import de.ui.sushi.fs.webdav.WebdavConnection;
import de.ui.sushi.fs.webdav.WebdavRoot;


public class MkColMethod extends WebdavMethod<Void> {
    public MkColMethod(WebdavRoot root, String path) {
        super(root, "MKCOL", path);
    }

    @Override
    public Void processResponse(WebdavConnection conection, HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
        	throw new StatusException(response.getStatusLine());
        }
        return null;
    }
}