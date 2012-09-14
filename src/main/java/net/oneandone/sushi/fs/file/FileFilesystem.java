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
package net.oneandone.sushi.fs.file;

import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.io.OS;

import java.io.File;
import java.net.URI;

public class FileFilesystem extends Filesystem {
    private final FileRoot[] roots;

    public FileFilesystem(World world, String name) {
        super(world, new Features(true, true, world.os != OS.WINDOWS, world.os != OS.WINDOWS, true, false, false), name);

        File[] rootFiles;

        rootFiles = File.listRoots();
        roots = new FileRoot[rootFiles.length];
        for (int i = 0; i < rootFiles.length; i++) {
            roots[i] = FileRoot.create(this, rootFiles[i]);
        }
    }

    public Node node(URI uri, Object extra) throws NodeInstantiationException {
        String authority;
        File file;
        String path;

        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkHierarchical(uri);
        authority = uri.getAuthority();
        if (authority != null) {
            throw new NodeInstantiationException(uri, "invalid root: unexpected authority " + authority);
        }
        path = uri.getPath();
        if (!path.startsWith(SEPARATOR_STRING)) {
            throw new NodeInstantiationException(uri, "missing initial separator " + SEPARATOR_STRING);
        }
        // note that the URI may contain a tailing slash, but turning it into a file will remove the slash;
        // getAbsolute is needed to add the current drive on windows if the URI path omitted the drive letter
        file = new File(uri).getAbsoluteFile();
        return new FileNode(getRoot(file), file.toPath());
    }

    public FileRoot getRoot(File file) {
        String path;
        FileRoot result;

        path = file.getPath();
        result = lookupRoot(path);
        if (result == null) {
            throw new IllegalArgumentException(path);
        }
        return result;
    }

    public FileRoot lookupRoot(String filePath) {
        filePath = filePath.toUpperCase();
        for (FileRoot root : roots) {
            if (filePath.startsWith(root.getAbsolute())) {
                return root;
            }
        }
        return null;
    }
}
