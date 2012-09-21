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
package net.oneandone.sushi.fs.zip;

import net.oneandone.sushi.archive.Archive;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Root;

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
        separator = Filesystem.SEPARATOR_STRING;
        prefix = path.length() == 0 ? "" : path + separator;
        length = prefix.length();
        result = new ArrayList<String>();
        while (e.hasMoreElements()) {
            entry = e.nextElement();
            name = entry.getName();
            if (name.length() > length && name.startsWith(prefix)) {
                idx = name.indexOf(separator, length);
                name = (idx == -1 ? name : name.substring(0, idx));
                if (!result.contains(name) && name.length() > 0 /* happens for "/" entries ... */) {
                    result.add(name);
                }
            }
        }
        return result;
    }

    public Manifest readManifest() throws IOException {
        Manifest result;

        try (InputStream src = node(Archive.MANIFEST, null).createInputStream()) {
            result = new Manifest(src);
        }
        return result;
    }
}
