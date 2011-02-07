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

package com.oneandone.sushi.fs.file;

import com.oneandone.sushi.fs.Features;
import com.oneandone.sushi.fs.Filesystem;
import com.oneandone.sushi.fs.World;
import com.oneandone.sushi.fs.Node;
import com.oneandone.sushi.fs.NodeInstantiationException;
import com.oneandone.sushi.io.OS;

import java.io.File;
import java.net.URI;

public class FileFilesystem extends Filesystem {
    private final FileRoot[] roots;

    public FileFilesystem(World io, String name) {
        super(io, File.separatorChar, new Features(true, true, io.os != OS.WINDOWS, io.os != OS.WINDOWS, true, false), name);

        File[] rootFiles;

        rootFiles = File.listRoots();
        roots = new FileRoot[rootFiles.length];
        for (int i = 0; i < rootFiles.length; i++) {
            roots[i] = new FileRoot(this, rootFiles[i].getAbsolutePath());
        }
    }

    public Node node(URI uri, Object extra) throws NodeInstantiationException {
        String authority;
        File file;
        String path;
        String separator;

        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkHierarchical(uri);
        authority = uri.getAuthority();
        if (authority != null) {
            throw new NodeInstantiationException(uri, "invalid root: unexpected authority " + authority);
        }
        path = uri.getPath();
        separator = getSeparator();
        if (!path.startsWith(separator)) {
            throw new NodeInstantiationException(uri, "missing initial separator " + separator);
        }
        // note that the URI may contain a tailing slash, but turning it into a file will remove the slash
        file = new File(uri);
        return new FileNode(getRoot(file), file);
    }

    public FileRoot getRoot(File file) {
        String path;

        path = file.getPath();
        for (FileRoot root : roots) {
            if (path.startsWith(root.getId())) {
                return root;
            }
        }
        throw new IllegalArgumentException(path);
    }
}
