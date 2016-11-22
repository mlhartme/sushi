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

import net.oneandone.sushi.fs.http.HttpConnection;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.StatusException;
import net.oneandone.sushi.fs.http.model.Header;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.fs.http.model.StatusCode;

import java.io.IOException;

/**
 * The output stream is added manually by the user.
 * See https://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html#PUT
 */
public class Put extends Method<Void> {
    public Put(HttpNode resource) {
        super("PUT", resource);
    }

    @Override
    protected void contentLength() {
        addRequestHeader(Header.TRANSFER_ENCODING, HttpConnection.CHUNK_CODING);
    }

    @Override
    public Void process(HttpConnection connection, Response response) throws IOException {
    	int status;

    	status = response.getStatusLine().code;
        if (status != StatusCode.OK && status != StatusCode.NO_CONTENT && status != StatusCode.CREATED) {
        	throw new StatusException(response.getStatusLine());
        }
        return null;
    }
}