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

import com.oneandone.sushi.fs.webdav.WebdavConnection;
import com.oneandone.sushi.fs.webdav.WebdavNode;
import com.oneandone.sushi.fs.webdav.WebdavRoot;
import com.oneandone.sushi.xml.Namespace;
import com.oneandone.sushi.xml.Xml;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.protocol.HTTP;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class Method<T> {
    public static final Namespace DAV = Namespace.getNamespace("D", "DAV:");
    public static final String XML_PROP = "prop";
    public static final String XML_RESPONSE = "response";

    
    //--
    
    protected final WebdavRoot root;

    private BasicHttpEntityEnclosingRequest request;
    
    public Method(String method, WebdavNode resource) {
        this.root = resource.getRoot();
        this.request = new BasicHttpEntityEnclosingRequest(method, resource.getEncodedPath());
    }

    //--
    
    public void setRequestHeader(String name, String value) {
    	request.addHeader(name, value);
    }
    
    public void setRequestEntity(Document body) throws IOException {
        ByteArrayOutputStream serialized;
        
        serialized = new ByteArrayOutputStream();
       	getXml().serializer.serialize(new DOMSource(body), new StreamResult(serialized));
    	request.setEntity(new ByteArrayEntity(serialized.toByteArray()));
    }

    //--
    
    public String getUri() {
    	return request.getRequestLine().getUri();
    }
    
    public Xml getXml() {
        return root.getFilesystem().getIO().getXml();
    }
    
    //--

    public T invoke() throws IOException {
    	return response(request());
    }

    public WebdavConnection request() throws IOException {
        WebdavConnection conn;
        
        conn = root.allocate();
    	setRequestHeader("Expires", "0");
        setRequestHeader("Pragma", "no-cache");
        setRequestHeader("Cache-control", "no-cache");
        setRequestHeader("Cache-store", "no-store");
        setRequestHeader(HTTP.USER_AGENT, "Sushi Webdav");
        setContentHeader();
        root.send(conn, request);
        return conn;
    }

    public T response(WebdavConnection connection) throws IOException {
        HttpResponse response;

        response = null;
        try {
            response = root.receive(connection);
            return processResponse(connection, response);
        } finally {
        	done(response, connection);
        }
    }

    protected void setContentHeader() {
        HttpEntity entity;
        
        entity = ((HttpEntityEnclosingRequest)request).getEntity();
        if (entity == null) {
            request.addHeader(HTTP.CONTENT_LEN, "0");
            return;
        }
        if (entity.isChunked() || entity.getContentLength() < 0) {
        	throw new IllegalStateException();
        }
        request.addHeader(HTTP.CONTENT_LEN, Long.toString(entity.getContentLength()));
        if (entity.getContentType() != null) {
            request.addHeader(entity.getContentType()); 
        }
        if (entity.getContentEncoding() != null) {
            request.addHeader(entity.getContentEncoding()); 
        }
    	
    }
    
    // TODO: connection argument needed for GetMethod ...
    public abstract T processResponse(WebdavConnection connection, HttpResponse response) throws IOException;

    protected void done(HttpResponse response, WebdavConnection conn) throws IOException {
    	root.free(response, conn);
    }
}