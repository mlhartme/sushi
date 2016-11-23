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

public class GenericResponse {
    public final StatusLine statusLine;
    public final HeaderList headerList;
    public final byte[] body;
    public final Response response;
    public final HttpConnection connection;

    public GenericResponse(StatusLine statusLine, HeaderList headerList, byte[] body, Response response, HttpConnection connection) {
        this.statusLine = statusLine;
        this.headerList = headerList;
        this.body = body;
        this.response = response;
        this.connection = connection;
    }
}
