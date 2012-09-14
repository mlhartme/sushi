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
package net.oneandone.sushi.fs;

import java.io.IOException;
import java.net.URI;

/** Indicates a problem creating a node, either because the URI is incorrect or the underlying services reported an error. */
public class NodeInstantiationException extends IOException {
    public final URI uri;

    public NodeInstantiationException(URI uri, String msg) {
        super(uri + ": " + msg);

        if (uri == null) {
            throw new IllegalArgumentException();
        }
        this.uri = uri;
    }

    public NodeInstantiationException(URI uri, String message, Throwable cause) {
        this(uri, message);
        initCause(cause);
    }
}
