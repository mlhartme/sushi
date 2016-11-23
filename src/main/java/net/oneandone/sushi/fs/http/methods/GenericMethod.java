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

import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.http.HttpConnection;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.MovedPermanentlyException;
import net.oneandone.sushi.fs.http.StatusException;
import net.oneandone.sushi.fs.http.io.ChunkedOutputStream;
import net.oneandone.sushi.fs.http.model.Body;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.fs.http.model.StatusCode;
import net.oneandone.sushi.fs.http.model.StatusLine;
import net.oneandone.sushi.io.Buffer;

import java.io.IOException;
import java.io.OutputStream;

public class GenericMethod extends Method<GenericResponse> {
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

    //--

    public GenericMethod(String method, HttpNode resource) {
        super(method, resource);
    }

    @Override
    public GenericResponse process(HttpConnection connection, Response response) throws IOException {
        Body body;
        byte[] array;
        Buffer buffer;

        body = response.getBody();
        if (body == null) {
            array = null;
        } else {
            buffer = resource.getWorld().getBuffer();
            synchronized (buffer) {
                array = buffer.readBytes(body.content);
            }
        }
        return new GenericResponse(response.getStatusLine(), response.getHeaderList(), array);
    }
}
