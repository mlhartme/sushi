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

package net.sf.beezle.sushi.fs.file;

import net.sf.beezle.sushi.fs.Filesystem;
import net.sf.beezle.sushi.fs.Root;
import net.sf.beezle.sushi.util.Strings;

import java.io.File;


public class FileRoot implements Root<FileNode> {
    public static FileRoot create(FileFilesystem filesystem, File file) {
        return new FileRoot(filesystem, file, file.getAbsolutePath(),
                Strings.removeStart(file.toURI().toString(), "file:"));
    }

    private final FileFilesystem filesystem;
    private final File file;
    private final String absolute;
    private final String id;
    
    public FileRoot(FileFilesystem filesystem, File file, String absolute, String id) {
        this.filesystem = filesystem;
        this.file = file;
        this.absolute = absolute;
        this.id = id;
        if (!id.endsWith(Filesystem.URI_SEPARATOR)) {
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
        return new FileNode(this, new File(file, path));
    }
}
