/*
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
import net.oneandone.sushi.fs.http.StatusException;
import net.oneandone.sushi.fs.http.io.ChunkedOutputStream;
import net.oneandone.sushi.xml.Builder;
import net.oneandone.sushi.xml.Namespace;
import net.oneandone.sushi.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class Method {
    public static final Namespace DAV = Namespace.getNamespace("D", "DAV:");
    public static final String XML_PROP = "prop";
    public static final String XML_RESPONSE = "response";

    public static InputStream get(HttpNode resource) throws IOException {
        return Request.streamResponse(resource, "GET", null, StatusCode.OK);
    }

    public static InputStream post(HttpNode resource, Body body) throws IOException {
        return Request.streamResponse(resource, "POST", body, StatusCode.OK, StatusCode.CREATED);
    }

    public static String head(HttpNode resource, String header) throws IOException {
        Request head;
        Response response;
        int status;

        head = new Request("HEAD", resource);
        response = head.request();
        status = response.getStatusLine().code;
        switch (status) {
            case StatusCode.OK:
                return header == null ? null : response.getHeaderList().getFirstValue(header);
            default:
                throw StatusException.forResponse(resource, response);
        }
    }

    public static void proppatch(HttpNode resource, Property property) throws IOException {
        Xml xml;
        Document document;
        Element set;
        Element prop;
        Request proppatch;
        Response response;
        List<MultiStatus> lst;
        MultiStatus ms;

        xml = resource.getWorld().getXml();
        document = xml.getBuilder().createDocument("propertyupdate", DAV);
        set = Builder.element(document.getDocumentElement(), "set", DAV);
        prop = Builder.element(set, XML_PROP, DAV);
        property.addXml(prop);
        proppatch = new Request("PROPPATCH", resource);
        response = proppatch.request(Body.forDom(xml.getSerializer(), document));

        switch (response.getStatusLine().code) {
            case StatusCode.OK:
                return;
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.MULTI_STATUS:
                lst = MultiStatus.fromResponse(resource.getWorld().getXml(), response.getBodyBytes());
                ms = MultiStatus.lookupOne(lst, property.getName());
                if (ms.status != StatusCode.OK) {
                    throw new StatusException(resource, response.getHeaderList(), new StatusLine(StatusLine.HTTP_1_1, ms.status), null);
                }
                return;
            default:
                throw StatusException.forResponse(resource, response);
        }
    }

    public static List<MultiStatus> propfind(HttpNode resource, Name name, int depth) throws IOException {
        Xml xml;
        Document document;
        Builder builder;
        Request propfind;
        Response response;

        xml = resource.getWorld().getXml();
        builder = xml.getBuilder();
        synchronized (builder) {
            document = builder.createDocument("propfind", DAV);
        }
        name.addXml(Builder.element(document.getDocumentElement(), XML_PROP, DAV));
        propfind = new Request("PROPFIND", resource);
        propfind.addRequestHeader("Depth", String.valueOf(depth));
        response = propfind.request(Body.forDom(xml.getSerializer(), document));

        switch (response.getStatusLine().code) {
            case StatusCode.MULTI_STATUS:
                return MultiStatus.fromResponse(resource.getWorld().getXml(), response.getBodyBytes());
            case StatusCode.BAD_REQUEST: // TODO
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.NOT_FOUND:
                throw new FileNotFoundException(resource);
            default:
                throw StatusException.forResponse(resource, response);
        }
    }

    public static void move(HttpNode source, HttpNode destination, boolean overwrite) throws IOException {
        Request move;
        Response response;

        move = new Request("MOVE", source);
        move.addRequestHeader("Destination", destination.getUri().toString());
        move.addRequestHeader("Overwrite", overwrite ? "T" : "F");
        response = move.request();
        switch (response.getStatusLine().code) {
            case StatusCode.NO_CONTENT:
            case StatusCode.CREATED:
                return;
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.NOT_FOUND:
                throw new FileNotFoundException(source);
            default:
                throw StatusException.forResponse(source, response);
        }
    }

    public static void mkcol(HttpNode resource) throws IOException {
        Request mkcol;
        Response response;

        mkcol = new Request("MKCOL", resource);
        response = mkcol.request();
        if (response.getStatusLine().code != StatusCode.CREATED) {
            throw StatusException.forResponse(resource, response);
        }
    }

    public static void delete(HttpNode resource) throws IOException {
        Request delete;
        Response response;

        delete = new Request("DELETE", resource);
        response = delete.request();
        switch (response.getStatusLine().code) {
            case StatusCode.OK:
            case StatusCode.NO_CONTENT:
                // success
                return;
            case StatusCode.MOVED_PERMANENTLY:
                throw new MovedPermanentlyException();
            case StatusCode.NOT_FOUND:
                throw new FileNotFoundException(resource);
            default:
                throw StatusException.forResponse(resource, response);
        }
    }

    /** See https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#PUT */
    public static OutputStream put(HttpNode resource) throws IOException {
        Request put;
        HttpConnection connection;

        put = new Request("PUT", resource);
        put.addRequestHeader(Header.TRANSFER_ENCODING, HttpConnection.CHUNK_CODING);
        connection = put.open(null);
        return new ChunkedOutputStream(connection.getOutputStream()) {
            private boolean closed = false;
            @Override
            public void close() throws IOException {
                Response reponse;
                int code;

                if (closed) {
                    return;
                }
                closed = true;
                super.close();
                reponse = put.finish(connection);
                code = reponse.getStatusLine().code;
                if (code != StatusCode.OK && code != StatusCode.NO_CONTENT && code != StatusCode.CREATED) {
                    throw StatusException.forResponse(resource, reponse);
                }
            }
        };
    }

    public static byte[] patch(HttpNode resource, Body body) throws IOException {
        Request patch;
        Response response;

        patch = new Request("PATCH", resource);
        response = patch.request(body);
        if (response.getStatusLine().code != StatusCode.OK && response.getStatusLine().code != StatusCode.CREATED) {
            throw StatusException.forResponse(resource, response);
        }
        return response.getBodyBytes();
    }

    private Method() {
    }
}
