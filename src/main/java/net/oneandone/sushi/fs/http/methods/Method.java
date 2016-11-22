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
import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.fs.http.model.RequestLine;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.fs.http.model.StatusCode;
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

    //--

    protected final HttpNode resource;

    public final RequestLine requestline;
    public final HeaderList headerList;
    public final Body body;


    public Method(String method, HttpNode resource) {
        this(method, resource, null);
    }

    public Method(String method, HttpNode resource, Body body) {
        this.resource = resource;
        this.headerList = new HeaderList();
        this.requestline = new RequestLine(method, resource.getRequestPath());
        this.body = body;

        // prepare header
        resource.getRoot().addDefaultHeader(headerList);
        contentLength();
        if (body != null) {
            if (body.type != null) {
                headerList.add(body.type);
            }
            if (body.encoding != null) {
                headerList.add(body.encoding);
            }
        }
    }

    public void addRequestHeader(String name, String value) {
    	headerList.add(name, value);
    }

    public String getUri() {
    	return requestline.uri;
    }

    public List<MultiStatus> multistatus(Response response) throws IOException {
        return MultiStatus.fromResponse(resource.getWorld().getXml(), response);
    }

    //-- main api

    public T invoke() throws IOException {
    	return response(request());
    }

    public HttpConnection request() throws IOException {
        HttpConnection connection;

        connection = resource.getRoot().allocate();
        try {
            connection.sendRequest(requestline, headerList, body);
        } catch (IOException e) {
            try {
                connection.close();
                resource.getRoot().free(connection);
            } catch (IOException e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
        return connection;
    }

    public T response(HttpConnection connection) throws IOException {
        Response response;
        T result;

        response = receive(connection);
        try {
            result = process(connection, response);
        } catch (IOException e) {
            try {
                free(response, connection);
            } catch (Exception e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
        freeOnSuccess(response, connection);
        return result;
    }

    public abstract T process(HttpConnection connection, Response response) throws IOException;

    //--

    /** CAUTION when overriding this method: it's called during super class construction, i.e. you class is not initialized yet! */
    protected void contentLength() {
        if (body == null) {
            headerList.add(Header.CONTENT_LENGTH, "0");
            return;
        }
        if (body.chunked || body.length < 0) {
        	throw new IllegalStateException();
        }
        headerList.add(Header.CONTENT_LENGTH, Long.toString(body.length));
    }

    protected void freeOnSuccess(Response response, HttpConnection connection) throws IOException {
        free(response, connection);
    }

    protected void free(Response response, HttpConnection connection) throws IOException {
        if (response.close()) {
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

    protected boolean hasBody(Response response) {
        int status;

        status = response.getStatusLine().code;
        return status >= StatusCode.OK
                && status != StatusCode.NO_CONTENT
                && status != StatusCode.NOT_MODIFIED
                && status != StatusCode.RESET_CONTENT;
    }

    private Response receive(HttpConnection connection) throws IOException {
        Response response;

        do {
            try {
                response = connection.receiveResponseHeader();
            } catch (IOException e) {
                try {
                    connection.close();
                    resource.getRoot().free(connection);
                } catch (IOException e2) {
                    e.addSuppressed(e2);
                }
                throw e;
            }
            try {
                if (hasBody(response)) {
                    connection.receiveResponseBody(response);
                }
            } catch (IOException e) {
                try {
                    free(response, connection);
                } catch (IOException e2) {
                    e.addSuppressed(e2);
                }
                throw e;
            }

        } while (response.getStatusLine().code < StatusCode.OK);
        return response;
    }
}