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
package net.oneandone.sushi.archive;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.io.Buffer;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Represents a Zip or Jar file. For Zip files, the manifest is null. */
public class Archive {
    public static final String META_INF = "META-INF";
    public static final String MANIFEST = META_INF + "/MANIFEST.MF";

    public static Archive createZip(World world) {
        return new Archive(world.getMemoryFilesystem().root().node("", null), null);
    }

    public static Archive loadZip(Node src) throws IOException {
        return createZip(src.getWorld()).read(src);
    }

    public static Archive createJar(World world) {
        return new Archive(world.getMemoryFilesystem().root().node("", null), new Manifest());
    }

    public static Archive loadJar(Node src) throws IOException {
        return createJar(src.getWorld()).read(src);
    }

    private static String getPath(ZipEntry entry) {
        String path;
        
        path = entry.getName();
        if (entry.isDirectory()) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
    
    //--
    
    public final Node data;

    /** null for zip files, not null for jars */
    public final Manifest manifest;

    public final String pathRoot;

    public Archive(Node data, Manifest manifest) {
        this(data, manifest, "");
    }

    public Archive(Node data, Manifest manifest, String pathRoot) {
        this.data = data;
        this.manifest = manifest;
        this.pathRoot = pathRoot;
    }

    /** @return this */
    public Archive read(Node file) throws IOException {
        Buffer buffer;
        ZipEntry entry;
        Node node;
        
        buffer = file.getWorld().getBuffer();
        try (ZipInputStream zip = new ZipInputStream(file.createInputStream())) {
            while (true) {
                entry = zip.getNextEntry();
                if (entry == null) {
                    break;
                }
                node = data.join(getPath(entry));
                if ("".equals(node.getPath())) {
                    continue;
                }
                if (entry.isDirectory()) {
                    node.mkdirsOpt();
                } else if (isManifest(node)) {
                    mergeManifest(new Manifest(zip));
                    zip.closeEntry();
                } else {
                    node.getParent().mkdirsOpt();
                    buffer.copy(zip, node);
                    zip.closeEntry();
                }
            }
        }
        return this;
    }

    private boolean isManifest(Node node) {
        return manifest != null && MANIFEST.equals(node.getPath());        
    }
    
    public void mergeManifest(Manifest rightManifest) {
        Map<String, Attributes> rightSections;
        Attributes left;
        
        manifest.getMainAttributes().putAll(rightManifest.getMainAttributes());
        rightSections = rightManifest.getEntries();
        for (String name : rightSections.keySet()) {
            left = manifest.getAttributes(name);
            if (left == null) {
                left = new Attributes();
                manifest.getEntries().put(name, left);
            }
            left.putAll(rightSections.get(name));
        }
    }

    public Archive save(Node dest) throws IOException {
        try (OutputStream out = dest.createOutputStream()) {
            save(out);
        }
        return this;
    }

    public Archive save(OutputStream dest) throws IOException {
        List<Node> content;
        List<Node> files;
        
        try (ZipOutputStream out = new ZipOutputStream(dest)) {
            if (manifest != null) {
                out.putNextEntry(new ZipEntry(MANIFEST));
                manifest.write(out);
                out.closeEntry();
            }
            content = data.find("**/*");
            files = new ArrayList<>();
            // directories first - jar does not extract files into non-existing directories
            for (Node node : content) {
                if (isManifest(node)) {
                    throw new ArchiveException("manifest file not allowed");
                } else if (node.isFile()) {
                    files.add(node);
                } else {
                    out.putNextEntry(new ZipEntry(Strings.removeLeft(node.getPath() + "/", pathRoot)));
                    out.closeEntry();
                }
            }
            for (Node file : files) {
                try (InputStream in = file.createInputStream()) {
                    out.putNextEntry(new ZipEntry(Strings.removeLeft(file.getPath(), pathRoot)));
                    file.getWorld().getBuffer().copy(in, out);
                    out.closeEntry();
                }
            }
        }
        return this;
    }
}
