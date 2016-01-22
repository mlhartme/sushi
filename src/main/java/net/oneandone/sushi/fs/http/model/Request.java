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

import net.oneandone.sushi.fs.http.io.AsciiOutputStream;

import java.io.IOException;
import java.util.Iterator;

public class Request {
    private final RequestLine requestline;
    private final HeaderList headerList;

    private Body body;

    public Request(String method, String uri) {
        this.headerList = new HeaderList();
        this.requestline = new RequestLine(method, uri, StatusLine.HTTP_1_1);
    }

    public String getUri() {
        return requestline.uri;
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

    public void write(AsciiOutputStream dest) throws IOException {
        String value;

        requestline.write(dest);
        for (Header header : headerList) {
            dest.writeAscii(header.name);
            dest.writeAscii(": ");
            value = header.value;
            if (value != null) {
                dest.writeAscii(value);
            }
            dest.writeAsciiLn();
        }
        dest.writeAsciiLn();
    }

}
