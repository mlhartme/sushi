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
import net.oneandone.sushi.fs.http.MovedException;
import net.oneandone.sushi.fs.http.StatusException;
import net.oneandone.sushi.fs.http.model.Response;

import java.io.IOException;

public class Move extends Method<Void> {
    public Move(HttpNode source, HttpNode destination, boolean overwrite) {
        super("MOVE", source);
        addRequestHeader("Destination", destination.getURI().toString());
        addRequestHeader("Overwrite", overwrite ? "T" : "F");
    }

    @Override
    public Void process(HttpConnection conection, Response response) throws IOException {
    	switch (response.getStatusLine().statusCode) {
    	case STATUSCODE_NO_CONTENT:
    	case STATUSCODE_CREATED:
    		return null;
    	case STATUSCODE_MOVED_PERMANENTLY:
    		throw new MovedException();
        case STATUSCODE_NOT_FOUND:
            throw new FileNotFoundException(resource);
    	default:
        	throw new StatusException(response.getStatusLine());
        }
    }
}