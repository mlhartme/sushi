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

import net.oneandone.sushi.fs.webdav.MovedException;
import net.oneandone.sushi.fs.webdav.StatusException;
import net.oneandone.sushi.fs.webdav.WebdavConnection;
import net.oneandone.sushi.fs.webdav.WebdavNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import java.io.IOException;

public class Move extends Method<Void> {
    public Move(WebdavNode source, WebdavNode destination, boolean overwrite) {
        super("MOVE", source);
        setRequestHeader("Destination", destination.getInternalURI().toString());
        setRequestHeader("Overwrite", overwrite ? "T" : "F");
    }
    
    @Override
    public Void processResponse(WebdavConnection conection, HttpResponse response) throws IOException {
    	switch (response.getStatusLine().getStatusCode()) {
    	case HttpStatus.SC_NO_CONTENT:
    	case HttpStatus.SC_CREATED:
    		return null;
    	case HttpStatus.SC_MOVED_PERMANENTLY:
    		throw new MovedException();
    	default:
        	throw new StatusException(response.getStatusLine());
        }
    }
}