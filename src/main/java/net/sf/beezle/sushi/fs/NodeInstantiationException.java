/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.beezle.sushi.fs;

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
