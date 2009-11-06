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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import de.ui.sushi.archive.Archive;
import de.ui.sushi.fs.Root;

public class ZipRoot implements Root {
    private final ZipFilesystem filesystem;
    private final ZipFile zip;
    
    public ZipRoot(ZipFilesystem filesystem, ZipFile zip) {
        this.filesystem = filesystem;
        this.zip = zip;
    }

    @Override
    public boolean equals(Object obj) {
        ZipRoot root;
        
        if (obj instanceof ZipRoot) {
            root = (ZipRoot) obj;
            return filesystem == root.filesystem && zip.equals(root.zip);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return zip.hashCode();
    }

    public ZipFile getZip() {
        return zip;
    }
    
    public ZipFilesystem getFilesystem() {
        return filesystem;
    }

    public long getLastModified() {
        return new File(zip.getName()).lastModified();
    }
    
    public String getId() {
        return new File(zip.getName()).toURI() + "!/";
    }

    public ZipNode node(String path) {
        return new ZipNode(this, path);
    }
    
    public Manifest readManifest() throws IOException {
        InputStream src;
        Manifest result;
        
        src = node(Archive.MANIFEST).createInputStream();
        result = new Manifest(src);
        src.close();
        return result;
    }

    //-- capabilities
    
    public boolean canLink() {
        return false;
    }
}
