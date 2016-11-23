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
package net.oneandone.sushi.fs.http.model;

import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.http.HttpConnection;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.MovedPermanentlyException;
import net.oneandone.sushi.fs.http.MovedTemporarilyException;
import net.oneandone.sushi.fs.http.MultiStatus;
import net.oneandone.sushi.fs.http.Name;
import net.oneandone.sushi.fs.http.Property;
import net.oneandone.sushi.fs.http.StatusException;
import net.oneandone.sushi.fs.http.io.ChunkedOutputStream;
import net.oneandone.sushi.io.Buffer;
import net.oneandone.sushi.xml.Builder;
import net.oneandone.sushi.xml.Namespace;
import net.oneandone.sushi.xml.Serializer;
import net.oneandone.sushi.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class GenericMethod {
    public static InputStream get(HttpNode resource) throws IOException {
        GenericMethod get;
        Response response;

        get = new GenericMethod("GET", resource, true);
        response = get.response(get.request(false, null));
        if (response.getStatusLine().code == StatusCode.OK) {
            return new FilterInputStream(response.getBody().content) {
                private boolean freed = false;

                @Override
                public void close() throws IOException {
                    if (!freed) {
                        freed = true;
                        get.free(response);
                    }
                    super.close();
                }
            };
        } else {
            get.free(response);
            switch (response.getStatusLine().code) {
                case StatusCode.MOVED_TEMPORARILY:
                    throw new MovedTemporarilyException(response.getHeaderList().getFirstValue("Location"));
                case StatusCode.NOT_FOUND:
                case StatusCode.GONE:
                case StatusCode.MOVED_PERMANENTLY:
                    throw new FileNotFoundException(resource);
                default:
                    throw new StatusException(response.getStatusLine());
            }
        }
    }

    public static String head(HttpNode resource, String header) throws IOException {
        GenericMethod head;
        Response response;
        int status;

        head = new GenericMethod("HEAD", resource);
        response = head.response(head.request(false, null));
        status = response.getStatusLine().code;
        switch (status) {
            case StatusCode.OK:
                return header == null ? null : response.getHeaderList().getFirstValue(header);
            default:
                throw new StatusException(response.getStatusLine());
        }
    }

    public static void proppatch(HttpNode resource, Property property) throws IOException {
        Xml xml;
        Document document;
        Element set;
        Element prop;
        GenericMethod proppatch;
        Response response;
        List<MultiStatus> lst;
        MultiStatus ms;

        xml = resource.getWorld().getXml();
        document = xml.getBuilder().createDocument("propertyupdate", DAV);
        set = Builder.element(document.getDocumentElement(), "set" , DAV);
        prop = Builder.element(set, XML_PROP, DAV);
        property.addXml(prop);
        proppatch = new GenericMethod("PROPPATCH", resource);
        response = proppatch.response(proppatch.request(false, body(xml.getSerializer(), document)));

        switch (response.getStatusLine().code) {
            case StatusCode.OK:
                return;
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.MULTI_STATUS:
                lst = proppatch.multistatus(response.getBodyBytes());
                ms = MultiStatus.lookupOne(lst, property.getName());
                if (ms.status != StatusCode.OK) {
                    throw new StatusException(new StatusLine(StatusLine.HTTP_1_1, ms.status));
                }
                return;
            default:
                throw new StatusException(response.getStatusLine());
        }
    }

    public static List<MultiStatus> propfind(HttpNode resource, Name name, int depth) throws IOException {
        Xml xml;
        Document document;
        Builder builder;
        GenericMethod propfind;
        Response response;

        xml = resource.getWorld().getXml();
        builder = xml.getBuilder();
        synchronized (builder) {
            document = builder.createDocument("propfind", DAV);
        }
        name.addXml(Builder.element(document.getDocumentElement(), XML_PROP, DAV));
        propfind = new GenericMethod("PROPFIND", resource);
        propfind.addRequestHeader("Depth", String.valueOf(depth));
        response = propfind.response(propfind.request(false, body(xml.getSerializer(), document)));

        switch (response.getStatusLine().code) {
            case StatusCode.MULTI_STATUS:
                return propfind.multistatus(response.getBodyBytes());
            case StatusCode.BAD_REQUEST: // TODO
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.NOT_FOUND:
                throw new FileNotFoundException(resource);
            default:
                throw new StatusException(response.getStatusLine());
        }
    }

    public static void move(HttpNode source, HttpNode destination, boolean overwrite) throws IOException {
        GenericMethod move;
        StatusLine result;

        move = new GenericMethod("MOVE", source);
        move.addRequestHeader("Destination", destination.getUri().toString());
        move.addRequestHeader("Overwrite", overwrite ? "T" : "F");
        result = move.response(move.request(false, null)).getStatusLine();
        switch (result.code) {
            case StatusCode.NO_CONTENT:
            case StatusCode.CREATED:
                return;
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.NOT_FOUND:
                throw new FileNotFoundException(source);
            default:
                throw new StatusException(result);
        }
    }

    public static void mkcol(HttpNode resource) throws IOException {
        GenericMethod mkcol;
        StatusLine line;

        mkcol = new GenericMethod("MKCOL", resource);
        line = mkcol.response(mkcol.request(false, null)).getStatusLine();
        if (line.code != StatusCode.CREATED) {
            throw new StatusException(line);
        }
    }

    public static void delete(HttpNode resource) throws IOException {
        GenericMethod delete;
        StatusLine result;

        delete = new GenericMethod("DELETE", resource);
        result = delete.response(delete.request(false, null)).getStatusLine();
        switch (result.code) {
            case StatusCode.NO_CONTENT:
                // success
                return;
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.NOT_FOUND:
                throw new FileNotFoundException(resource);
            default:
                throw new StatusException(result);
        }
    }

    /** See https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#PUT */
    public static OutputStream put(HttpNode resource) throws IOException {
        GenericMethod method;
        HttpConnection connection;

        method = new GenericMethod("PUT", resource);
        connection = method.request(true, null);
        return new ChunkedOutputStream(connection.getOutputStream()) {
            private boolean closed = false;
            @Override
            public void close() throws IOException {
                StatusLine statusLine;
                int code;

                if (closed) {
                    return;
                }
                closed = true;
                super.close();
                statusLine = method.response(connection).getStatusLine();
                code = statusLine.code;
                if (code != StatusCode.OK && code != StatusCode.NO_CONTENT && code != StatusCode.CREATED) {
                    throw new StatusException(statusLine);
                }
            }
        };
    }

    public static byte[] post(HttpNode resource, Body body) throws IOException {
        GenericMethod post;
        Response response;

        post = new GenericMethod("POST", resource);
        response = post.response(post.request(false, body));
        if (response.getStatusLine().code != StatusCode.OK) {
            throw new StatusException(response.getStatusLine());
        }
        return response.getBodyBytes();
    }

    //--

    public static final Namespace DAV = Namespace.getNamespace("D", "DAV:");
    public static final String XML_PROP = "prop";
    public static final String XML_RESPONSE = "response";

    //--

    private final HttpNode resource;
    private final String method;
    private final String uri;
    private final HeaderList headerList;

    private final boolean bodyStream;

    public GenericMethod(String method, HttpNode resource) {
        this(method, resource, false);
    }

    public GenericMethod(String method, HttpNode resource, boolean bodyStream) {
        this.resource = resource;
        this.headerList = new HeaderList();
        this.method = method;
        this.uri = resource.getRequestPath();
        this.bodyStream = bodyStream;
        resource.getRoot().addDefaultHeader(headerList);
    }

    public void addRequestHeader(String name, String value) {
        headerList.add(name, value);
    }

    public String getUri() {
        return uri;
    }

    public List<MultiStatus> multistatus(byte[] responseBody) throws IOException {
        return MultiStatus.fromResponse(resource.getWorld().getXml(), responseBody);
    }

    //-- main api

    public HttpConnection request(boolean putChunked, Body body) throws IOException {
        HttpConnection connection;

        if (body == null) {
            if (putChunked) {
                headerList.add(Header.TRANSFER_ENCODING, HttpConnection.CHUNK_CODING);
            } else {
                headerList.add(Header.CONTENT_LENGTH, "0");
            }
        } else {
            if (body.chunked || body.length < 0) {
                throw new IllegalStateException();
            }
            headerList.add(Header.CONTENT_LENGTH, Long.toString(body.length));
            if (body.type != null) {
                headerList.add(body.type);
            }
            if (body.encoding != null) {
                headerList.add(body.encoding);
            }
        }
        connection = resource.getRoot().allocate();
        try {
            connection.sendRequest(method, uri, headerList, body);
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

    public Response response(HttpConnection connection) throws IOException {
        Response response;
        Body body;
        Buffer buffer;

        response = receive(connection);
        if (bodyStream) {
            // don't free
        } else {
            try {
                body = response.getBody();
                if (body != null) {
                    buffer = resource.getWorld().getBuffer();
                    synchronized (buffer) {
                        response.setBodyBytes(buffer.readBytes(body.content));
                    }
                }
            } finally {
                free(response);
            }
        }
        return response;
    }

    //--

    protected void free(Response response) throws IOException {
        if (response.close()) {
            response.connection.close();
        }
        resource.getRoot().free(response.connection);
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

    /** https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.3 */
    private boolean hasBody(Response response) {
        int status;

        status = response.getStatusLine().code;
        if ("HEAD".equals(method)) {
            return false;
        } else {
            return status >= StatusCode.OK && status != StatusCode.NO_CONTENT && status != StatusCode.NOT_MODIFIED && status != StatusCode.RESET_CONTENT;
        }
    }

    public Response receive(HttpConnection connection) throws IOException {
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
                    free(response);
                } catch (IOException e2) {
                    e.addSuppressed(e2);
                }
                throw e;
            }

        } while (response.getStatusLine().code < StatusCode.OK);
        return response;
    }
}
