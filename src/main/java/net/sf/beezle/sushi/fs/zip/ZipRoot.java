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

package net.sf.beezle.sushi.fs.zip;

import net.sf.beezle.sushi.archive.Archive;
import net.sf.beezle.sushi.fs.Root;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipRoot implements Root<ZipNode> {
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

    public ZipNode node(String path, String encodedQuery) {
        if (encodedQuery != null) {
            throw new IllegalArgumentException(encodedQuery);
        }
        return new ZipNode(this, path);
    }

    // TODO: cache?
    public List<String> list(String path) {
        ZipEntry entry;
        Enumeration<? extends ZipEntry> e;
        String name;
        String separator;
        String prefix;
        int length;
        List<String> result;
        int idx;

        e = zip.entries();
        separator = getFilesystem().getSeparator();
        prefix = path.length() == 0 ? "" : path + separator;
        length = prefix.length();
        result = new ArrayList<String>();
        while (e.hasMoreElements()) {
            entry = e.nextElement();
            name = entry.getName();
            if (name.length() > length && name.startsWith(prefix)) {
                idx = name.indexOf(separator, length);
                name = (idx == -1 ? name : name.substring(0, idx));
                if (!result.contains(name)) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    public Manifest readManifest() throws IOException {
        InputStream src;
        Manifest result;

        src = node(Archive.MANIFEST, null).createInputStream();
        result = new Manifest(src);
        src.close();
        return result;
    }
}
