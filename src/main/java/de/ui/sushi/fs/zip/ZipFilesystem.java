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
import de.ui.sushi.fs.RootPathException;
import de.ui.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URI;
import java.util.zip.ZipFile;

public class ZipFilesystem extends Filesystem {
    private static final String ZIP_SEPARATOR = "!/";

    public ZipFilesystem(IO io, String name) {
        super(io, '/', new Features(false, false, false, false, false, false), name);
    }

    public ZipRoot root(String fileUri) throws RootPathException {
        Node jar;

        fileUri = fileUri.substring(0, fileUri.length() - ZIP_SEPARATOR.length());
        jar = getIO().node(fileUri);
        if (!(jar instanceof FileNode)) {
            throw new RootPathException("file node expected: " + fileUri);
        }
        try {
            return root((FileNode) jar);
        } catch (IOException e) {
            throw new RootPathException(e);
        }
    }

    public ZipNode node(FileNode jar, String path) throws IOException {
        return root(jar).node(path);
    }

    public ZipRoot root(FileNode jar) throws IOException {
        return new ZipRoot(this, new ZipFile(jar.getAbsolute()));
    }

    public ZipNode node(URI uri) throws RootPathException {
        String schemeSpecific;
        String path;

        checkOpaque(uri);
        schemeSpecific = uri.getSchemeSpecificPart();
        path = after(schemeSpecific, ZIP_SEPARATOR);
        if (path == null) {
            throw new RootPathException(uri, "unexpected opaque uri: " + schemeSpecific);
        }
        if (path.endsWith(getSeparator())) {
            throw new RootPathException(uri, "invalid tailing " + getSeparator());
        }
        if (path.startsWith(getSeparator())) {
            throw new RootPathException(uri, "invalid heading " + getSeparator());
        }
        return root(schemeSpecific.substring(0, schemeSpecific.length() - path.length())).node(path);
    }
}
