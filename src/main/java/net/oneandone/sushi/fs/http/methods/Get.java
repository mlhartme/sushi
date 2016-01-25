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
import net.oneandone.sushi.fs.http.StatusException;
import net.oneandone.sushi.fs.http.model.Response;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Get extends Method<InputStream> {
    public Get(HttpNode resource) {
        super("GET", resource);
    }

    @Override
    public InputStream processResponse(final HttpConnection connection, final Response response) throws IOException {
    	int status;

        status = response.getStatusLine().statusCode;
        switch (status) {
        case STATUSCODE_OK:
        	return new FilterInputStream(response.getBody().content) {
                private boolean freed = false;

        		@Override
        		public void close() throws IOException {
                    if (!freed) {
                        freed = true;
                        resource.getRoot().free(response, connection);
                    }
                    super.close();
        		}
        	};
        case STATUSCODE_NOT_FOUND:
        case STATUSCODE_GONE:
        case STATUSCODE_MOVED_PERMANENTLY:
            resource.getRoot().free(response, connection);
            throw new FileNotFoundException(resource);
        default:
            resource.getRoot().free(response, connection);
        	throw new StatusException(response.getStatusLine());
        }
    }

    @Override
    public void processResponseFinally(Response response, HttpConnection connection) {
    	// do nothing - the resulting stream perform the close
    }
}