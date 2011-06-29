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

import net.sf.beezle.sushi.util.Splitter;
import net.sf.beezle.sushi.util.Strings;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public abstract class Filesystem {
    /**
     * Separator in URIs and paths. You'll rearely use this constant, in particular, it doesn't make sense to
     * use it in URI or path constants.
     */
	public static final String SEPARATOR = "/";

    /**
     * Separator in URIs and paths. You'll rearely use this constant, in particular, it doesn't make sense to
     * use it in URI or path constants.
     */
	public static final char SEPARATOR_CHAR = '/';

    private final World world;
    private final Features features;
    private final String scheme;

    public Filesystem(World world, Features features, String scheme) {
        this.world = world;
        this.features = features;
        this.scheme = scheme;
    }

    public World getWorld() {
        return world;
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
        if (path.length() > 0) {
            if (!path.startsWith(SEPARATOR)) {
                throw new NodeInstantiationException(uri, "missing initial separator " + SEPARATOR);
            }
            path = path.substring(1);
            if (path.endsWith(SEPARATOR)) {
                throw new NodeInstantiationException(uri, "invalid tailing " + SEPARATOR);
            }
        }
        return path;
    }

    //--

    public String join(String... names) {
        return join("", Arrays.asList(names));
    }

    public String join(String head, List<String> paths) {
        StringBuilder builder;

        builder = new StringBuilder(head);
        for (String path : paths) {
            if (path.length() > 0) {
                if (path.startsWith(SEPARATOR)) {
                    throw new IllegalArgumentException(path);
                }
                if (builder.length() > 0) {
                    builder.append(SEPARATOR);
                }
                builder.append(path);
            }
        }
        normalize(builder);
        return builder.toString();
    }

    public void normalize(StringBuilder builder) {
        int idx;
        int prev;

        idx = 0;
        while (true) {
            idx = builder.indexOf(".", idx);
            if (idx == -1) {
                break;
            }
            if (idx + 1 < builder.length() && builder.charAt(idx + 1) == '.') {
                if (idx + 2 == builder.length() || builder.charAt(idx + 2) == SEPARATOR_CHAR) {
                    if (idx == 0) {
                        throw new IllegalArgumentException(builder.toString());
                    }
                    if (builder.charAt(idx - 1) == SEPARATOR_CHAR) {
                        prev = builder.lastIndexOf(SEPARATOR, idx - 2) + 1; // ok for -1
                        builder.delete(prev, idx + 1);
                        idx = prev;
                        if (builder.charAt(idx) == SEPARATOR_CHAR) {
                            builder.deleteCharAt(idx);
                        }
                        continue;
                    }
                }
            }
            if (idx == 0 || builder.charAt(idx - 1) == SEPARATOR_CHAR) {
                if (idx + 1 == builder.length() || builder.charAt(idx + 1) == SEPARATOR_CHAR) {
                    builder.deleteCharAt(idx);
                    if (idx < builder.length() && builder.charAt(idx) == SEPARATOR_CHAR) {
                        builder.deleteCharAt(idx);
                    } else if (idx > 0 && builder.charAt(idx - 1) == SEPARATOR_CHAR) {
                        builder.deleteCharAt(--idx);
                    }
                    continue;
                }
            }
            idx++;
        }
        // TODO: this is not part of java.net.URI's normalization
        for (int i = builder.length() - 1; i > 0; i--) {
            if (builder.charAt(i) == SEPARATOR_CHAR && builder.charAt(i - 1) == SEPARATOR_CHAR) {
                builder.deleteCharAt(i);
            }
        }
    }
}
