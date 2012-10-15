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

import net.oneandone.sushi.fs.webdav.WebdavConnection;
import net.oneandone.sushi.fs.webdav.WebdavNode;
import net.oneandone.sushi.xml.Namespace;
import net.oneandone.sushi.xml.Serializer;
import net.oneandone.sushi.xml.Xml;
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

    protected final WebdavNode resource;

    private final BasicHttpEntityEnclosingRequest request;

    private final boolean head;

    public Method(String method, WebdavNode resource) {
        this(method, resource, false);
    }

    public Method(String method, WebdavNode resource, boolean head) {
        this.resource = resource;
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
            serializer.serialize(new DOMSource(body), new StreamResult(serialized), true);
        }
    	request.setEntity(new ByteArrayEntity(serialized.toByteArray()));
    }

    //--

    public String getUri() {
    	return request.getRequestLine().getUri();
    }

    public Xml getXml() {
        return resource.getRoot().getFilesystem().getWorld().getXml();
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
        conn = resource.getRoot().allocate();
        resource.getRoot().send(conn, request);
        return conn;
    }

    public T response(WebdavConnection connection) throws IOException {
        HttpResponse response;

        response = resource.getRoot().receive(connection, head);
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
    	resource.getRoot().free(response, conn);
    }
}