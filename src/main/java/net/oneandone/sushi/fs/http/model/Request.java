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

import net.oneandone.sushi.fs.http.HttpConnection;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.io.Buffer;

import java.io.IOException;
import java.util.List;

public class Request {
    private final HttpNode resource;
    private final String method;
    private final String uri;
    private final HeaderList headerList;

    private final boolean bodyStream;

    public Request(String method, HttpNode resource) {
        this(method, resource, false);
    }

    public Request(String method, HttpNode resource, boolean bodyStream) {
        this.resource = resource;
        this.headerList = new HeaderList();
        this.method = method;
        this.uri = resource.getRequestPath();
        this.bodyStream = bodyStream;
        resource.getRoot().addDefaultHeader(headerList);
    }

    public String getUri() {
        return uri;
    }

    public void addRequestHeader(String name, String value) {
        headerList.add(name, value);
    }

    public Response request(Body body) throws IOException {
        bodyHeader(body);
        return request(open(body));
    }

    public Response request(HttpConnection connection) throws IOException {
        Response response;
        Body body;
        Buffer buffer;

        response = reponse(connection);
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

    public void bodyHeader(Body body) throws IOException {
        if (body == null) {
            headerList.add(Header.CONTENT_LENGTH, "0");
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
    }

    public HttpConnection open(Body body) throws IOException {
        HttpConnection connection;

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

    private Response reponse(HttpConnection connection) throws IOException {
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

    //--

    public List<MultiStatus> multistatus(byte[] responseBody) throws IOException {
        return MultiStatus.fromResponse(resource.getWorld().getXml(), responseBody);
    }

    protected void free(Response response) throws IOException {
        if (response.close()) {
            response.connection.close();
        }
        resource.getRoot().free(response.connection);
    }

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

}
