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

package com.oneandone.sushi.fs.timemachine;

import com.oneandone.sushi.fs.ExistsException;
import com.oneandone.sushi.fs.Root;
import com.oneandone.sushi.fs.file.FileNode;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TimeMachineRoot implements Root<TimeMachineNode> {
    public static TimeMachineRoot create(TimeMachineFilesystem fs, FileNode root) throws ExistsException, FileNotFoundException {
        return new TimeMachineRoot(fs,
                root.join("Backups.backupdb"), root.join(".HFS+ Private Directory Data\r"));
    }

    private final TimeMachineFilesystem filesystem;
    private final FileNode root;
    private final FileNode shared;

    public TimeMachineRoot(TimeMachineFilesystem filesystem, FileNode root, FileNode shared) throws ExistsException, FileNotFoundException {
        root.checkDirectory();
        shared.checkDirectory();

        this.filesystem = filesystem;
        this.root = root;
        this.shared = shared;
    }

    @Override
    public boolean equals(Object obj) {
        TimeMachineRoot tm;

        if (obj instanceof TimeMachineRoot) {
            tm = (TimeMachineRoot) obj;
            return filesystem == tm.filesystem && root.equals(tm.root) && shared.equals(tm.shared);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    public TimeMachineFilesystem getFilesystem() {
        return filesystem;
    }

    public String getId() {
        return "//" + root.getParent().getPath() + "!";
    }

    public FileNode resolve(FileNode node) throws IOException {
        String str;

        if (node.isFile() && node.length() == 0) {
            str = root.exec("stat", "-c", "%h", node.getFile().getAbsolutePath()).trim();
            if (str.length() > 1) {
                return shared.join("dir_" + str);
            }
        }
        return node;
    }

    public TimeMachineNode node(String path, String encodedQuery) {
        FileNode node;

        if (encodedQuery != null) {
            throw new IllegalArgumentException(encodedQuery);
        }
        node = root;
        for (String segment : filesystem.split(path)) {
            try {
                node = resolve(node.join(segment));
            } catch (IOException e) {
                throw new IllegalStateException("TODO", e);
            }
        }
        return new TimeMachineNode(this, node, path);
    }
}
