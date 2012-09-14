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
package net.oneandone.sushi.fs.timemachine;

import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Root;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;

public class TimeMachineRoot implements Root<TimeMachineNode> {
    public static TimeMachineRoot create(TimeMachineFilesystem fs, FileNode root) throws ExistsException, DirectoryNotFoundException {
        return new TimeMachineRoot(fs,
                root.join("Backups.backupdb"), root.join(".HFS+ Private Directory Data\r"));
    }

    private final TimeMachineFilesystem filesystem;
    private final FileNode root;
    private final FileNode shared;

    public TimeMachineRoot(TimeMachineFilesystem filesystem, FileNode root, FileNode shared) throws ExistsException, DirectoryNotFoundException {
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
            str = root.exec("stat", "-c", "%h", node.toPath().toAbsolutePath().toString()).trim();
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
        for (String segment : Filesystem.SEPARATOR.split(path)) {
            try {
                node = resolve(node.join(segment));
            } catch (IOException e) {
                throw new IllegalStateException("TODO", e);
            }
        }
        return new TimeMachineNode(this, node, path);
    }
}
