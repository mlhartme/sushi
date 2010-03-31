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

    public Node node(URI uri) throws RootPathException {
        String path;

        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(uri.toString());
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException(uri.toString());
        }
        if (uri.isOpaque()) {
            return opaqueNode(uri.getSchemeSpecificPart());
        } else {
            path = uri.getPath();
            if (!path.startsWith(separator)) {
                throw new RootPathException(uri.toString());
            }
            path = path.substring(separator.length());
            if (path.endsWith(separator)) {
                throw new RootPathException("invalid tailing " + separator);
            }
            return root(uri.getAuthority()).node(path);
        }
    }

    /**
     * Returns the specified root. The root is not necessarily new. You'll normally not use this method directly.
     *
     * @param authority as specified in the uri. For opaque uris the authority is the schemeSpecific part without
     * schemePath (and thus never null)
     */
    public abstract Root root(String authority) throws RootPathException;

    public Node opaqueNode(String schemeSpecific) throws RootPathException {
        String path;

        path = opaquePath(schemeSpecific);
        if (path == null) {
            throw new RootPathException("unexpected opaque uri: " + schemeSpecific);
        }
        if (path.endsWith(getSeparator())) {
            throw new RootPathException("invalid tailing " + getSeparator());
        }
        if (path.startsWith(getSeparator())) {
            throw new RootPathException("invalid heading " + getSeparator());
        }
        return root(schemeSpecific.substring(0, schemeSpecific.length() - path.length())).node(path);
    }

    /** override this to use opaque uris
     * @param schemeSpecific*/
    public String opaquePath(String schemeSpecific) throws RootPathException {
        return null;
    }

    /** Helper Method for opaquePath implementations */
    public String after(String schemeSpecific, String separator) throws RootPathException {
        int idx;

        idx = schemeSpecific.indexOf(separator);
        if (idx == -1) {
            throw new RootPathException("missing '" + separator + "': " + schemeSpecific);
        }
        return schemeSpecific.substring(idx + separator.length());
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
