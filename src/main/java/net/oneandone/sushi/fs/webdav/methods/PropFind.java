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
package net.oneandone.sushi.fs.webdav.methods;

import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.webdav.MovedException;
import net.oneandone.sushi.fs.webdav.MultiStatus;
import net.oneandone.sushi.fs.webdav.Name;
import net.oneandone.sushi.fs.webdav.StatusException;
import net.oneandone.sushi.fs.webdav.WebdavConnection;
import net.oneandone.sushi.fs.webdav.WebdavNode;
import net.oneandone.sushi.xml.Builder;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.List;

public class PropFind extends Method<List<MultiStatus>> {
    public PropFind(WebdavNode resource, Name name, int depth) throws IOException {
    	super("PROPFIND", resource);

        Document document;
        Builder builder;

        setRequestHeader("Depth", String.valueOf(depth));
        builder = getXml().getBuilder();
        synchronized (builder) {
            document = builder.createDocument("propfind", DAV);
        }
		name.addXml(Builder.element(document.getDocumentElement(), XML_PROP, DAV));
        setRequestEntity(document);
    }

    @Override
    public List<MultiStatus> processResponse(WebdavConnection connection, HttpResponse response) throws IOException {
        switch (response.getStatusLine().getStatusCode()) {
        case HttpStatus.SC_MULTI_STATUS:
            return MultiStatus.fromResponse(getXml(), response);
        case HttpStatus.SC_BAD_REQUEST: // TODO
        case HttpStatus.SC_MOVED_PERMANENTLY:
        	throw new MovedException();
        case HttpStatus.SC_NOT_FOUND:
        	throw new FileNotFoundException(resource);
        default:
        	throw new StatusException(response.getStatusLine());
        }
    }
}
