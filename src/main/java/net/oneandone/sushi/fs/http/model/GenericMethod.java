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
        GenericResponse response;

        get = new GenericMethod("GET", resource, true);
        response = get.response(get.request(false, null));
        if (response.statusLine.code == StatusCode.OK) {
            return new FilterInputStream(response.response.getBody().content) {
                private boolean freed = false;

                @Override
                public void close() throws IOException {
                    if (!freed) {
                        freed = true;
                        get.free(response.response, response.connection);
                    }
                    super.close();
                }
            };
        } else {
            get.free(response.response, response.connection);
            switch (response.statusLine.code) {
                case StatusCode.MOVED_TEMPORARILY:
                    throw new MovedTemporarilyException(response.headerList.getFirstValue("Location"));
                case StatusCode.NOT_FOUND:
                case StatusCode.GONE:
                case StatusCode.MOVED_PERMANENTLY:
                    throw new FileNotFoundException(resource);
                default:
                    throw new StatusException(response.statusLine);
            }
        }
    }

    public static String head(HttpNode resource, String header) throws IOException {
        GenericMethod head;
        GenericResponse response;
        int status;

        head = new GenericMethod("HEAD", resource);
        response = head.response(head.request(false, null));
        status = response.statusLine.code;
        switch (status) {
            case StatusCode.OK:
                return header == null ? null : response.headerList.getFirstValue(header);
            default:
                throw new StatusException(response.statusLine);
        }
    }

    public static void proppatch(HttpNode resource, Property property) throws IOException {
        Xml xml;
        Document document;
        Element set;
        Element prop;
        GenericMethod proppatch;
        GenericResponse response;
        List<MultiStatus> lst;
        MultiStatus ms;

        xml = resource.getWorld().getXml();
        document = xml.getBuilder().createDocument("propertyupdate", DAV);
        set = Builder.element(document.getDocumentElement(), "set" , DAV);
        prop = Builder.element(set, XML_PROP, DAV);
        property.addXml(prop);
        proppatch = new GenericMethod("PROPPATCH", resource);
        response = proppatch.response(proppatch.request(false, body(xml.getSerializer(), document)));

        switch (response.statusLine.code) {
            case StatusCode.OK:
                return;
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.MULTI_STATUS:
                lst = proppatch.multistatus(response.body);
                ms = MultiStatus.lookupOne(lst, property.getName());
                if (ms.status != StatusCode.OK) {
                    throw new StatusException(new StatusLine(StatusLine.HTTP_1_1, ms.status));
                }
                return;
            default:
                throw new StatusException(response.statusLine);
        }
    }

    public static List<MultiStatus> propfind(HttpNode resource, Name name, int depth) throws IOException {
        Xml xml;
        Document document;
        Builder builder;
        GenericMethod propfind;
        GenericResponse response;

        xml = resource.getWorld().getXml();
        builder = xml.getBuilder();
        synchronized (builder) {
            document = builder.createDocument("propfind", DAV);
        }
        name.addXml(Builder.element(document.getDocumentElement(), XML_PROP, DAV));
        propfind = new GenericMethod("PROPFIND", resource);
        propfind.addRequestHeader("Depth", String.valueOf(depth));
        response = propfind.response(propfind.request(false, body(xml.getSerializer(), document)));

        switch (response.statusLine.code) {
            case StatusCode.MULTI_STATUS:
                return propfind.multistatus(response.body);
            case StatusCode.BAD_REQUEST: // TODO
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.NOT_FOUND:
                throw new FileNotFoundException(resource);
            default:
                throw new StatusException(response.statusLine);
        }
    }

    public static void move(HttpNode source, HttpNode destination, boolean overwrite) throws IOException {
        GenericMethod move;
        StatusLine result;

        move = new GenericMethod("MOVE", source);
        move.addRequestHeader("Destination", destination.getUri().toString());
        move.addRequestHeader("Overwrite", overwrite ? "T" : "F");
        result = move.response(move.request(false, null)).statusLine;
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
        line = mkcol.response(mkcol.request(false, null)).statusLine;
        if (line.code != StatusCode.CREATED) {
            throw new StatusException(line);
        }
    }

    public static void delete(HttpNode resource) throws IOException {
        GenericMethod delete;
        StatusLine result;

        delete = new GenericMethod("DELETE", resource);
        result = delete.response(delete.request(false, null)).statusLine;
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
                statusLine = method.response(connection).statusLine;
                code = statusLine.code;
                if (code != StatusCode.OK && code != StatusCode.NO_CONTENT && code != StatusCode.CREATED) {
                    throw new StatusException(statusLine);
                }
            }
        };
    }

    public static byte[] post(HttpNode resource, Body body) throws IOException {
        GenericMethod post;
        GenericResponse response;

        post = new GenericMethod("POST", resource);
        response = post.response(post.request(false, body));
        if (response.statusLine.code != StatusCode.OK) {
            throw new StatusException(response.statusLine);
        }
        return response.body;
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

    public GenericResponse process(HttpConnection connection, Response response) throws IOException {
        Body body;
        Buffer buffer;
        byte[] bytes;

        body = response.getBody();
        if (body == null) {
            bytes = null;
        } else {
            if (bodyStream) {
                bytes = null;
            } else {
                buffer = resource.getWorld().getBuffer();
                synchronized (buffer) {
                    bytes = buffer.readBytes(body.content);
                }
            }
        }
        return new GenericResponse(response.getStatusLine(), response.getHeaderList(), bytes, response, connection);
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

    public GenericResponse invoke(Body body) throws IOException {
        return response(request(false, body));
    }

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

    public GenericResponse response(HttpConnection connection) throws IOException {
        Response response;
        GenericResponse result;

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
        if (!bodyStream) {
            free(response, connection);
        }
        return result;
    }

    //--

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
