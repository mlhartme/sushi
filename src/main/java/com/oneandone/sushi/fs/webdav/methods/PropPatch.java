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

import com.oneandone.sushi.fs.webdav.MovedException;
import com.oneandone.sushi.fs.webdav.MultiStatus;
import com.oneandone.sushi.fs.webdav.Name;
import com.oneandone.sushi.fs.webdav.Property;
import com.oneandone.sushi.fs.webdav.StatusException;
import com.oneandone.sushi.fs.webdav.WebdavConnection;
import com.oneandone.sushi.fs.webdav.WebdavNode;
import com.oneandone.sushi.xml.Builder;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicStatusLine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.List;

public class PropPatch extends Method<Void> {
    private final Name dest;

    public PropPatch(WebdavNode resource, Property property) throws IOException {
        super("PROPPATCH", resource);

        Document document;
        Element set;
        Element prop;

        this.dest = property.getName();

        document = getXml().builder.createDocument("propertyupdate", DAV);
        set = Builder.element(document.getDocumentElement(), "set" , DAV);
        prop = Builder.element(set, XML_PROP, DAV);
        property.addXml(prop);
        setRequestEntity(document);
    }

    @Override
    public Void processResponse(WebdavConnection connection, HttpResponse response) throws IOException {
        List<MultiStatus> lst;
        MultiStatus ms;
        
    	switch (response.getStatusLine().getStatusCode()) {
    	case HttpStatus.SC_OK:
    		return null;
        case HttpStatus.SC_MOVED_PERMANENTLY:
        	throw new MovedException();
    	case HttpStatus.SC_MULTI_STATUS:
    		lst = MultiStatus.fromResponse(getXml(), response);
    		ms = MultiStatus.lookupOne(lst, dest);
    		if (ms.status != HttpStatus.SC_OK) {
    			throw new StatusException(new BasicStatusLine(HttpVersion.HTTP_1_1, ms.status, null));
    		}
    		return null;
        default:
        	throw new StatusException(response.getStatusLine());
        }
    }
}