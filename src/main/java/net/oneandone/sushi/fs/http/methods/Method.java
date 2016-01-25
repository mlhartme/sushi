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
package net.oneandone.sushi.fs.http.methods;

import net.oneandone.sushi.fs.http.HttpConnection;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.MultiStatus;
import net.oneandone.sushi.fs.http.model.Body;
import net.oneandone.sushi.fs.http.model.Header;
import net.oneandone.sushi.fs.http.model.Request;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.xml.Namespace;
import net.oneandone.sushi.xml.Serializer;
import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public abstract class Method<T> {
    public static final Namespace DAV = Namespace.getNamespace("D", "DAV:");
    public static final String XML_PROP = "prop";
    public static final String XML_RESPONSE = "response";

    //-- RFC 1945 and 2518 status codes
    public static final int STATUSCODE_OK = 200;
    public static final int STATUSCODE_CREATED = 201;
    public static final int STATUSCODE_NO_CONTENT = 204;
    public static final int STATUSCODE_RESET_CONTENT = 205;
    public static final int STATUSCODE_MULTI_STATUS = 207;

    public static final int STATUSCODE_MOVED_PERMANENTLY = 301;
    public static final int STATUSCODE_NOT_MODIFIED = 304;

    public static final int STATUSCODE_BAD_REQUEST = 400;
    public static final int STATUSCODE_NOT_FOUND = 404;
    public static final int STATUSCODE_GONE = 410;

    //--

    protected final HttpNode resource;

    private final Request request;

    public Method(String method, HttpNode resource) {
        this(method, resource, null);
    }

    public Method(String method, HttpNode resource, Body body) {
        this.resource = resource;
        this.request = new Request(method, resource.getAbsPath(), body);
        if (body != null) {
            if (body.type != null) {
                request.headerList.add(body.type);
            }
            if (body.encoding != null) {
                request.headerList.add(body.encoding);
            }
        }
    }

    public void addRequestHeader(String name, String value) {
    	request.headerList.add(name, value);
    }

    public String getUri() {
    	return request.requestline.uri;
    }

    public List<MultiStatus> multistatus(Response response) throws IOException {
        return MultiStatus.fromResponse(resource.getWorld().getXml(), response);
    }

    //--

    public T invoke() throws IOException {
    	return response(request());
    }

    public HttpConnection request() throws IOException {
        HttpConnection connection;

    	addRequestHeader("Expires", "0");
        addRequestHeader("Pragma", "no-cache");
        addRequestHeader("Cache-control", "no-cache");
        addRequestHeader("Cache-store", "no-store");
        addRequestHeader("User-Agent", "Sushi Http");
        contentLength();
        connection = resource.getRoot().allocate();
        resource.getRoot().send(connection, request);
        return connection;
    }

    public T response(HttpConnection connection) throws IOException {
        Response response;

        response = receive(connection);
        try {
            return processResponse(connection, response);
        } finally {
        	 processResponseFinally(response, connection);
        }
    }

    protected void contentLength() {
        Body body;

        body = request.body;
        if (body == null) {
            request.headerList.add(Header.CONTENT_LENGTH, "0");
            return;
        }
        if (body.chunked || body.length < 0) {
        	throw new IllegalStateException();
        }
        request.headerList.add(Header.CONTENT_LENGTH, Long.toString(body.length));
    }

    // TODO: connection argument needed for GetMethod ...
    public abstract T processResponse(HttpConnection connection, Response response) throws IOException;

    /** called after processResponse finished normally or with an exception */
    protected void processResponseFinally(Response response, HttpConnection connection) throws IOException {
        if (resource.getRoot().free(response)) {
            connection.close();
        }
    	resource.getRoot().free(connection);
    }

    //--

    public static Body body(Serializer serializer, Document body) {
        ByteArrayOutputStream serialized;
        byte[] bytes;

        serialized = new ByteArrayOutputStream();
        synchronized (serializer) {
            try {
                serializer.serialize(new DOMSource(body), new StreamResult(serialized), true);
            } catch (IOException e) {
                throw new IllegalStateException(e); // because we serialize into memory
            }
        }
        bytes = serialized.toByteArray();
        return new Body(null, null, bytes.length, new ByteArrayInputStream(bytes), false);
    }

    //--

    protected boolean receiveBody(Response response) {
        int status;

        status = response.getStatusLine().statusCode;
        return status >= Method.STATUSCODE_OK
                && status != Method.STATUSCODE_NO_CONTENT
                && status != Method.STATUSCODE_NOT_MODIFIED
                && status != Method.STATUSCODE_RESET_CONTENT;
    }

    private Response receive(HttpConnection connection) throws IOException {
        Response response;

        do {
            try {
                response = connection.receiveResponseHeader();
            } catch (IOException e) {
                connection.close();
                resource.getRoot().free(connection);
                throw e;
            }
            try {
                if (receiveBody(response)) {
                    connection.receiveResponseBody(response);
                }
            } catch (IOException | RuntimeException e) {
                if (resource.getRoot().free(response)) {
                    connection.close();
                }
                resource.getRoot().free(connection);
                throw e;
            }

        } while (response.getStatusLine().statusCode < Method.STATUSCODE_OK);
        return response;
    }
}