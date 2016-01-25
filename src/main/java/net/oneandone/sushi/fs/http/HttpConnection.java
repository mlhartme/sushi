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
package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.fs.http.io.AsciiInputStream;
import net.oneandone.sushi.fs.http.io.AsciiOutputStream;
import net.oneandone.sushi.fs.http.io.ChunkedInputStream;
import net.oneandone.sushi.fs.http.io.ChunkedOutputStream;
import net.oneandone.sushi.fs.http.model.Body;
import net.oneandone.sushi.fs.http.model.Header;
import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.fs.http.model.ProtocolException;
import net.oneandone.sushi.fs.http.model.Request;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.io.LineLogger;
import net.oneandone.sushi.io.LoggingAsciiInputStream;
import net.oneandone.sushi.io.LoggingAsciiOutputStream;
import net.oneandone.sushi.io.OpenInputStream;
import net.oneandone.sushi.io.OpenOutputStream;
import net.oneandone.sushi.io.WindowInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;

public class HttpConnection implements Closeable {
    public static HttpConnection open(Socket socket, HttpRoot root) throws IOException {
        int linger;
        int buffersize;
        InputStream input;
        OutputStream output;

        socket.setTcpNoDelay(root.getTcpNoDelay());
        socket.setSoTimeout(root.getSoTimeout());
        linger = root.getLinger();
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
        buffersize = Math.max(socket.getReceiveBufferSize(), 1024);
        input = socket.getInputStream();
        output = socket.getOutputStream();
        if (HttpFilesystem.WIRE.isLoggable(Level.FINE)) {
            input = new LoggingAsciiInputStream(input, new LineLogger(HttpFilesystem.WIRE, "<<< "));
            output = new LoggingAsciiOutputStream(output, new LineLogger(HttpFilesystem.WIRE, ">>> "));
		}
        return new HttpConnection(socket, new AsciiInputStream(input, buffersize), new AsciiOutputStream(output, buffersize));
    }

    private final Socket socket;
    private final AsciiInputStream input;
    private final AsciiOutputStream output;
    private boolean open;
    private final byte[] serializeBuffer;

    public HttpConnection(Socket socket, AsciiInputStream input, AsciiOutputStream output) {
    	this.socket = socket;
        this.input = input;
        this.output = output;
        this.open = true;
        this.serializeBuffer = new byte[4096];
    }

    //--

    public void sendRequestHeader(Request request) throws IOException {
        request.write(output);
    }

    public void sendRequestBody(Request request) throws IOException {
        if (request.getBody() != null) {
            serialize(output, request.getHeaderList(), request.getBody());
        }
    }

    public Response receiveResponseHeader() throws IOException {
        return Response.parse(input);
    }

    public void receiveResponseBody(Response response) throws IOException {
        response.setBody(deserialize(input, response.getHeaderList()));
    }

    public void flush() throws IOException {
        output.flush();
    }

    public void close() throws IOException {
        if (!open) {
            return;
        }
        open = false;
        socket.close();
    }

    public boolean isOpen() {
        return open;
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
        OutputStream result;
        int length;

        if (serializeLength(headerList) == CHUNKED) {
            result = new ChunkedOutputStream(dest);
        } else {
            result = new OpenOutputStream(dest);
        }

        // TODO: user buffer copy code
        synchronized (serializeBuffer) {
            try (InputStream in = body.content) {
                while (true) {
                    length = in.read(serializeBuffer);
                    if (length == -1) {
                        break;
                    }
                    result.write(serializeBuffer, 0, length);
                }
            }
        }
        result.close();
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

    private static Body deserialize(AsciiInputStream src, HeaderList list) throws IOException {
        long length;
        Header type;
        Header encoding;

        length = deserializeLength(list);
        type = list.getFirst(Header.CONTENT_TYPE);
        encoding = list.getFirst(Header.CONTENT_ENCODING);
        if (length == CHUNKED) {
            return new Body(type, encoding, -1, new ChunkedInputStream(src), true);
        } else if (length == IDENTITY) {
            return new Body(type, encoding, -1, new OpenInputStream(src), false);
        } else {
            return new Body(type, encoding, length, new WindowInputStream(src, length), false);
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