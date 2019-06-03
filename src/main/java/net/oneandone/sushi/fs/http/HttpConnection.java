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
package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.fs.http.io.AsciiInputStream;
import net.oneandone.sushi.fs.http.io.AsciiOutputStream;
import net.oneandone.sushi.fs.http.io.ChunkedInputStream;
import net.oneandone.sushi.fs.http.io.ChunkedOutputStream;
import net.oneandone.sushi.fs.http.model.Body;
import net.oneandone.sushi.fs.http.model.Header;
import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.fs.http.model.ProtocolException;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.io.Buffer;
import net.oneandone.sushi.io.OpenInputStream;
import net.oneandone.sushi.io.OpenOutputStream;
import net.oneandone.sushi.io.WindowInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpConnection implements Closeable {
    private final Socket socket;
    private final AsciiInputStream input;
    private final AsciiOutputStream output;
    private final byte[] bufferBytes;
    private final Buffer buffer;
    private boolean open;

    public HttpConnection(Socket socket, AsciiInputStream input, AsciiOutputStream output) {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.open = true;
        this.bufferBytes = new byte[4096];
        this.buffer = new Buffer(bufferBytes);
    }

    //--

    public void sendRequest(String method, String uri, HeaderList headerList, Body body) throws IOException {
        String value;

        output.writeRequestLine(method, uri);
        for (Header header : headerList) {
            output.writeAscii(header.name);
            output.writeAscii(": ");
            value = header.value;
            if (value != null) {
                output.writeAscii(value);
            }
            output.writeAsciiLn();
        }
        output.writeAsciiLn();

        if (body != null) {
            serialize(output, headerList, body);
        }
        output.flush();
    }

    public Response receiveResponseHeader() throws IOException {
        return Response.parse(this, input);
    }

    public void receiveResponseBody(Response response) throws IOException {
        response.setBody(deserialize(input, response.getHeaderList()));
    }

    public void close() throws IOException {
        if (open) {
            open = false;
            socket.close();
        }
    }

    public boolean isOpen() {
        return open;
    }


    public Buffer getBuffer() {
        return buffer;
    }

    //--

    public AsciiOutputStream getOutputStream() {
        return output;
    }

    @Override
    public String toString() {
        return "HttpConnection(" + socket.getPort() + ')';
    }

    //--

    public static final String CHUNK_CODING = "chunked";
    private static final String IDENTITY_CODING = "identity";
    private static final int IDENTITY = -1;
    private static final int CHUNKED = -2;

    public void serialize(AsciiOutputStream dest, HeaderList headerList, Body body) throws IOException {
        try (OutputStream result = (serializeLength(headerList) == CHUNKED ? new ChunkedOutputStream(bufferBytes, dest) : new OpenOutputStream(dest))) {
            try (InputStream in = body.content) {
                buffer.copy(in, result);
            }
        }
    }

    private static long serializeLength(HeaderList list) throws ProtocolException {
        Header transferEncoding;
        Header contentLength;

        transferEncoding = list.getFirst(Header.TRANSFER_ENCODING);
        if (transferEncoding != null) {
            return transferEncoding(transferEncoding);
        }
        contentLength = list.getFirst(Header.CONTENT_LENGTH);
        if (contentLength != null) {
            String s = contentLength.value;
            try {
                return Long.parseLong(s);
            } catch (final NumberFormatException e) {
                throw new ProtocolException("Invalid content length: " + s);
            }
        }
        return IDENTITY;
    }

    private Body deserialize(AsciiInputStream src, HeaderList list) throws IOException {
        long length;
        Header type;
        Header encoding;

        length = deserializeLength(list);
        type = list.getFirst(Header.CONTENT_TYPE);
        encoding = list.getFirst(Header.CONTENT_ENCODING);
        if (length == CHUNKED) {
            return new Body(type, encoding, -1, new ChunkedInputStream(src, buffer), true);
        } else if (length == IDENTITY) {
            return new Body(type, encoding, -1, new OpenInputStream(src), false);
        } else {
            return new Body(type, encoding, length, new WindowInputStream(src, length, buffer), false);
        }
    }

    private static long deserializeLength(HeaderList list) throws ProtocolException {
        Header encoding;
        Header length;

        encoding = list.getFirst(Header.TRANSFER_ENCODING);
        if (encoding != null) {
            return transferEncoding(encoding);
        }
        length = list.getFirst(Header.CONTENT_LENGTH);
        if (length != null) {
            try {
                return Long.parseLong(length.value);
            } catch (NumberFormatException e) {
                throw new ProtocolException("invalid content length: " + length.value, e);
            }
        }
        return IDENTITY;
    }

    private static long transferEncoding(Header transferEncodingHeader) throws ProtocolException {
        String value;

        value = transferEncodingHeader.value;
        if (CHUNK_CODING.equalsIgnoreCase(value)) {
            return CHUNKED;
        } else if (IDENTITY_CODING.equalsIgnoreCase(value)) {
            return IDENTITY;
        } else {
            throw new ProtocolException("unknown transfer encoding: " + value);
        }
    }
}