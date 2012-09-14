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

import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Root;
import net.oneandone.sushi.util.Strings;

import java.io.File;


public class FileRoot implements Root<FileNode> {
    public static FileRoot create(FileFilesystem filesystem, File file) {
        return new FileRoot(filesystem, file, file.getAbsolutePath().toUpperCase(),
                Strings.removeLeft(file.toURI().toString(), "file:").toUpperCase());
    }

    private final FileFilesystem filesystem;
    private final File file;
    /** file.getAbsolutePath().toUpperCase() */
    private final String absolute;
    private final String id;

    public FileRoot(FileFilesystem filesystem, File file, String absolute, String id) {
        this.filesystem = filesystem;
        this.file = file;
        this.absolute = absolute;
        this.id = id;
        if (!id.endsWith(Filesystem.SEPARATOR_STRING)) {
            throw new IllegalArgumentException(id);
        }
    }

    public FileFilesystem getFilesystem() {
        return filesystem;
    }

    public File getFile() {
        return file;
    }

    public String getAbsolute() {
        return absolute;
    }

    public String getId() {
        return id;
    }

    public FileNode node(String path, String encodedQuery) {
        if (encodedQuery != null) {
            throw new IllegalArgumentException(encodedQuery);
        }
    	if (File.separatorChar != Filesystem.SEPARATOR_CHAR) {
    		path = path.replace(Filesystem.SEPARATOR_CHAR, File.separatorChar);
    	}
        if (path.startsWith(Filesystem.SEPARATOR_STRING)) {
            throw new IllegalArgumentException();
        }
        return new FileNode(this, new File(file, path).toPath());
    }
}
