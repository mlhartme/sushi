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
package net.oneandone.sushi.fs.webdav.methods;

import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.webdav.StatusException;
import net.oneandone.sushi.fs.webdav.WebdavConnection;
import net.oneandone.sushi.fs.webdav.WebdavNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Get extends Method<InputStream> {
    public Get(WebdavNode resource) {
        super("GET", resource);
    }

    @Override
    public InputStream processResponse(final WebdavConnection connection, final HttpResponse response) throws IOException {
    	int status;

        status = response.getStatusLine().getStatusCode();
        switch (status) {
        case HttpStatus.SC_OK:
        	return new FilterInputStream(response.getEntity().getContent()) {
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
        case HttpStatus.SC_NOT_FOUND:
        case HttpStatus.SC_GONE:
        case HttpStatus.SC_MOVED_PERMANENTLY:
            resource.getRoot().free(response, connection);
            throw new FileNotFoundException(resource);
        default:
            resource.getRoot().free(response, connection);
        	throw new StatusException(response.getStatusLine());
        }
    }

    @Override
    public void processResponseFinally(HttpResponse response, WebdavConnection connection) {
    	// do nothing - the resulting stream perform the close
    }
}