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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import com.oneandone.sushi.fs.webdav.MovedException;
import com.oneandone.sushi.fs.webdav.MultiStatus;
import com.oneandone.sushi.fs.webdav.Name;
import com.oneandone.sushi.fs.webdav.StatusException;
import com.oneandone.sushi.fs.webdav.WebdavConnection;
import com.oneandone.sushi.fs.webdav.WebdavRoot;
import com.oneandone.sushi.xml.Builder;
import org.w3c.dom.Document;

public class PropFindMethod extends WebdavMethod<List<MultiStatus>> {
    public PropFindMethod(WebdavRoot root, String path, Name name, int depth) throws IOException {
    	super(root, "PROPFIND", path);
    	
        Document document;

        setRequestHeader("Depth", String.valueOf(depth));
        document = getXml().builder.createDocument("propfind", DAV);
		name.addXml(Builder.element(document.getDocumentElement(), XML_PROP, DAV));
        setRequestEntity(document);
    }

    @Override
    public List<MultiStatus> processResponse(WebdavConnection conection, HttpResponse response) throws IOException {
        switch (response.getStatusLine().getStatusCode()) {
        case HttpStatus.SC_MULTI_STATUS:
            return MultiStatus.fromResponse(getXml(), response);
        case HttpStatus.SC_MOVED_PERMANENTLY:
        	throw new MovedException();
        case HttpStatus.SC_NOT_FOUND:
        	throw new FileNotFoundException(getUri());
        default:
        	throw new StatusException(response.getStatusLine());
        }
    }
}
