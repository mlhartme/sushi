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

public class RequestLine {
    public final String protocol;
    public final String method;
    public final String uri;

    public RequestLine(String method, String uri, String protocol) {
        this.method = method;
        this.uri = uri;
        this.protocol = protocol;
    }

    public void write(AsciiOutputStream dest) throws IOException {
        dest.writeAscii(method);
        dest.writeAscii(' ');
        dest.writeAscii(uri);
        dest.writeAscii(' ');
        dest.writeAscii(protocol);
        dest.writeAsciiLn();
    }
}
