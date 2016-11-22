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
import net.oneandone.sushi.fs.http.model.Body;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.fs.http.model.StatusCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** the output stream is added manually by the user */
public class Post extends Method<byte[]> {
    public static byte[] run(HttpNode resource, Body body) throws IOException {
        return new Post(resource, body).invoke();
    }

    private Post(HttpNode resource, Body body) {
        super("POST", resource, body);
    }

    @Override
    public byte[] process(HttpConnection connection, Response response) throws IOException {
    	int status;
        ByteArrayOutputStream tmp;

    	status = response.getStatusLine().code;
        if (status != StatusCode.OK) {
        	throw new StatusException(response.getStatusLine());
        }
        tmp = new ByteArrayOutputStream();
        resource.getWorld().getBuffer().copy(response.getBody().content, tmp);
        return tmp.toByteArray();
    }
}