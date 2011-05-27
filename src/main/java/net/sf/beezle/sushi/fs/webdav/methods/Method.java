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

package net.sf.beezle.sushi.fs.webdav.methods;

import net.sf.beezle.sushi.fs.webdav.WebdavConnection;
import net.sf.beezle.sushi.fs.webdav.WebdavNode;
import net.sf.beezle.sushi.fs.webdav.WebdavRoot;
import net.sf.beezle.sushi.xml.Namespace;
import net.sf.beezle.sushi.xml.Serializer;
import net.sf.beezle.sushi.xml.Xml;
import org.apache.http.HttpEntity;
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

    private final BasicHttpEntityEnclosingRequest request;

    private final boolean head;

    public Method(String method, WebdavNode resource) {
        this(method, resource, false);
    }

    public Method(String method, WebdavNode resource, boolean head) {
        this.root = resource.getRoot();
        this.head = head;
        this.request = new BasicHttpEntityEnclosingRequest(method, resource.getAbsPath());
    }

    //--

    public void setRequestHeader(String name, String value) {
    	request.addHeader(name, value);
    }

    public void setRequestEntity(Document body) throws IOException {
        Serializer serializer;
        ByteArrayOutputStream serialized;

        serialized = new ByteArrayOutputStream();
        serializer = getXml().getSerializer();
        synchronized (serializer) {
            serializer.serialize(new DOMSource(body), new StreamResult(serialized));
        }
    	request.setEntity(new ByteArrayEntity(serialized.toByteArray()));
    }

    //--

    public String getUri() {
    	return request.getRequestLine().getUri();
    }

    public Xml getXml() {
        return root.getFilesystem().getWorld().getXml();
    }

    //--

    public T invoke() throws IOException {
    	return response(request());
    }

    public WebdavConnection request() throws IOException {
        WebdavConnection conn;

    	setRequestHeader("Expires", "0");
        setRequestHeader("Pragma", "no-cache");
        setRequestHeader("Cache-control", "no-cache");
        setRequestHeader("Cache-store", "no-store");
        setRequestHeader(HTTP.USER_AGENT, "Sushi Webdav");
        setContentHeader();
        conn = root.allocate();
        root.send(conn, request);
        return conn;
    }

    public T response(WebdavConnection connection) throws IOException {
        HttpResponse response;

        response = root.receive(connection, head);
        try {
            return processResponse(connection, response);
        } finally {
        	processResponseFinally(response, connection);
        }
    }

    protected void setContentHeader() {
        HttpEntity entity;

        entity = request.getEntity();
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

    /** called after processResponse finished normally or with an exception */
    protected void processResponseFinally(HttpResponse response, WebdavConnection conn) throws IOException {
    	root.free(response, conn);
    }
}