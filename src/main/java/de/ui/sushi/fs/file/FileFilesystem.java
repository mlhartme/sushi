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

package de.ui.sushi.fs.file;

import de.ui.sushi.fs.Features;
import de.ui.sushi.fs.Filesystem;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.RootPathException;
import de.ui.sushi.io.OS;

import java.io.File;
import java.net.URI;

public class FileFilesystem extends Filesystem {
    private final FileRoot[] roots;

    public FileFilesystem(IO io, String name) {
        super(io, File.separatorChar, new Features(true, true, io.os != OS.WINDOWS, io.os != OS.WINDOWS, true, false), name);

        File[] rootFiles;

        rootFiles = File.listRoots();
        roots = new FileRoot[rootFiles.length];
        for (int i = 0; i < rootFiles.length; i++) {
            roots[i] = new FileRoot(this, rootFiles[i].getAbsolutePath());
        }
    }

    public Node node(URI uri, Object extra) throws RootPathException {
        if (extra != null) {
            throw new RootPathException(uri, "unexpected extra argument: " + extra);
        }
        checkHierarchical(uri);
        return root(uri.getAuthority()).node(getCheckedPath(uri));
    }

    public FileRoot root(String authority) throws RootPathException {
        if (authority != null) {
            throw new RootPathException("invalid root: unexpected authority " + authority);
        }
        if (roots.length != 1) {
            throw new UnsupportedOperationException("TODO");
        }
        return roots[0];
    }
}
