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
import net.oneandone.sushi.fs.http.model.Response;

import java.io.IOException;

public class Head extends Method<Void> {
    public Head(HttpNode resource) {
        super("HEAD", resource, null);
    }

    @Override
    public Void processResponse(HttpConnection connection, Response response) throws IOException {
    	int status;

        status = response.getStatusLine().statusCode;
        switch (status) {
        case STATUSCODE_OK:
        	return null;
        default:
        	throw new StatusException(response.getStatusLine());
        }
    }

    @Override
    protected boolean receiveBody(Response response) {
        int status;

        status = response.getStatusLine().statusCode;
        if (status == Method.STATUSCODE_OK) {
            return false;
        } else {
            return super.receiveBody(response);
        }
    }
}