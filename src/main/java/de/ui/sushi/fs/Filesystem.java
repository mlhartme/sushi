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

package de.ui.sushi.fs;

import java.net.URI;
import java.util.List;

import de.ui.sushi.util.Strings;

public abstract class Filesystem {
    private final IO io;
    private final String separator;
    private final char separatorChar;
    private final Features features;
    private final String scheme;

    public Filesystem(IO io, char separatorChar, Features features, String scheme) {
        this.io = io;
        this.separator = String.valueOf(separatorChar);
        this.separatorChar = separatorChar;
        this.features = features;
        this.scheme = scheme;
    }

    public IO getIO() {
        return io;
    }

    public String getSeparator() {
        return separator;
    }

    public char getSeparatorChar() {
        return separatorChar;
    }

    public Features getFeatures() {
        return features;
    }

    public String getScheme() {
    	return scheme;
    }

    public abstract Node node(URI uri, Object extra) throws NodeInstantiationException;

    /** Helper Method for opaquePath implementations */
    public String after(String schemeSpecific, String separator) {
        int idx;

        idx = schemeSpecific.indexOf(separator);
        if (idx == -1) {
            return null;
        }
        return schemeSpecific.substring(idx + separator.length());
    }

    public void checkHierarchical(URI uri) throws NodeInstantiationException {
        if (uri.getFragment() != null) {
            throw new NodeInstantiationException(uri, "unexpected path fragment");
        }
        if (uri.getQuery() != null) {
            throw new NodeInstantiationException(uri, "unexpected query");
        }
        if (uri.isOpaque()) {
            throw new NodeInstantiationException(uri, "uri is not hierarchical");
        }
    }

    public void checkOpaque(URI uri) throws NodeInstantiationException {
        if (uri.getFragment() != null) {
            throw new NodeInstantiationException(uri, "unexpected path fragment");
        }
        if (uri.getQuery() != null) {
            throw new NodeInstantiationException(uri, "unexpected query");
        }
        if (!uri.isOpaque()) {
            throw new NodeInstantiationException(uri, "uri is not opqaue");
        }
    }

    public String getCheckedPath(URI uri) throws NodeInstantiationException {
        String path;

        path = uri.getPath();
        if (!path.startsWith(separator)) {
            throw new NodeInstantiationException(uri, "missing initial separator " + separator);
        }
        path = path.substring(separator.length());
        if (path.endsWith(separator)) {
            throw new NodeInstantiationException(uri, "invalid tailing " + separator);
        }
        return path;
    }

    //--

    public String join(String... names) {
        return Strings.join(separator, names);
    }

    public String join(String head, List<String> paths) {
        StringBuilder buffer;

        buffer = new StringBuilder(head);
        for (String path : paths) {
            if (path.startsWith(separator)) {
                throw new IllegalArgumentException(path);
            }
            // TODO: Svn nodes ...
            if (buffer.length() > 0) {
                buffer.append(separatorChar);
            }
            buffer.append(path);
        }
        return buffer.toString();
    }

    public List<String> split(String path) {
        return Strings.split(separator, path);
    }
}
