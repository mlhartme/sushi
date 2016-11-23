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
import net.oneandone.sushi.fs.http.io.AsciiInputStream;

import java.io.IOException;

public class Response {
    public static Response parse(HttpConnection connection, AsciiInputStream sessionBuffer) throws IOException {
        String str;
        StatusLine statusline;

        str = sessionBuffer.readLine();
        if (str == null) {
            throw new ProtocolException("response header expected, got eof");
        }
        statusline = StatusLine.parse(str);
        return new Response(connection, statusline, HeaderList.parse(sessionBuffer));
    }

    public final HttpConnection connection;
    private final StatusLine statusline;
    private final HeaderList headerList;
    private Body body;
    private byte[] bodyBytes;

    public Response(HttpConnection connection, StatusLine statusline, HeaderList headerList) {
        this.connection = connection;
        this.statusline = statusline;
        this.headerList = headerList;
    }

    public StatusLine getStatusLine() {
        return statusline;
    }


    public HeaderList getHeaderList() {
        return headerList;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public void setBodyBytes(byte[] bytes) {
        this.bodyBytes = bytes;
    }

    /** @return true if the response want's the connection losed */
    public boolean close() throws IOException {
        Header header;

        if (body != null) {
            body.content.close();
        }
        header = headerList.getFirst(Header.CONNECTION);
        return header != null && "close".equalsIgnoreCase(header.value);
    }
}
