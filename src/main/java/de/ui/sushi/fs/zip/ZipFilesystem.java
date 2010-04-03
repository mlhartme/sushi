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

package de.ui.sushi.fs.zip;

import de.ui.sushi.fs.Features;
import de.ui.sushi.fs.Filesystem;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.NodeInstantiationException;
import de.ui.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipFile;

public class ZipFilesystem extends Filesystem {
    private static final String ZIP_SEPARATOR = "!/";

    public ZipFilesystem(IO io, String name) {
        super(io, '/', new Features(false, false, false, false, false, false), name);
    }

    public ZipNode node(URI uri, Object extra) throws NodeInstantiationException {
        String schemeSpecific;
        String path;
        Node jar;

        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkOpaque(uri);
        schemeSpecific = uri.getSchemeSpecificPart();
        path = after(schemeSpecific, ZIP_SEPARATOR);
        if (path == null) {
            throw new NodeInstantiationException(uri, "missing '" + ZIP_SEPARATOR +"'");
        }
        if (path.endsWith(getSeparator())) {
            throw new NodeInstantiationException(uri, "invalid tailing " + getSeparator());
        }
        if (path.startsWith(getSeparator())) {
            throw new NodeInstantiationException(uri, "invalid heading " + getSeparator());
        }
        try {
            jar = getIO().node(schemeSpecific.substring(0, schemeSpecific.length() - path.length() - ZIP_SEPARATOR.length()));
        } catch (URISyntaxException e) {
            throw new NodeInstantiationException(uri, "invalid jar file in jar url", e);
        }
        if (!(jar instanceof FileNode)) {
            throw new NodeInstantiationException(uri, "file node expected, got: " + jar.getLocator());
        }
        try {
            return root((FileNode) jar).node(path);
        } catch (IOException e) {
            throw new NodeInstantiationException(uri, "io exception", e);
        }
    }

    public ZipRoot root(FileNode jar) throws IOException {
        return new ZipRoot(this, new ZipFile(jar.getAbsolute()));
    }

    public ZipNode node(FileNode jar, String path) throws IOException {
        return root(jar).node(path);
    }
}
