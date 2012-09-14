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

import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipFile;

public class ZipFilesystem extends Filesystem {
    private static final String ZIP_SEPARATOR = "!/";

    public ZipFilesystem(World world, String name) {
        super(world, new Features(false, false, false, false, false, false, false), name);
    }

    public ZipNode node(URI uri, Object extra) throws NodeInstantiationException {
        String encodedSchemeSpecific;
        String path;
        Node jar;

        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkOpaque(uri);
        encodedSchemeSpecific = uri.getRawSchemeSpecificPart();
        path = after(encodedSchemeSpecific, ZIP_SEPARATOR);
        if (path == null) {
            throw new NodeInstantiationException(uri, "missing '" + ZIP_SEPARATOR +"'");
        }
        if (path.endsWith(SEPARATOR_STRING)) {
            throw new NodeInstantiationException(uri, "invalid tailing " + SEPARATOR_STRING);
        }
        if (path.startsWith(SEPARATOR_STRING)) {
            throw new NodeInstantiationException(uri, "invalid heading " + SEPARATOR_STRING);
        }
        try {
            jar = getWorld().node(encodedSchemeSpecific.substring(0, encodedSchemeSpecific.length() - path.length() - ZIP_SEPARATOR.length()));
        } catch (URISyntaxException e) {
            throw new NodeInstantiationException(uri, "invalid jar file in jar url", e);
        }
        if (!(jar instanceof FileNode)) {
            throw new NodeInstantiationException(uri, "file node expected, got: " + jar.getURI());
        }
        try {
            return root((FileNode) jar).node(ZipNode.decodePath(path), null);
        } catch (IOException e) {
            throw new NodeInstantiationException(uri, "world exception", e);
        }
    }

    public ZipRoot root(FileNode jar) throws IOException {
        return new ZipRoot(this, new ZipFile(jar.getAbsolute()));
    }

    public ZipNode node(FileNode jar, String path) throws IOException {
        return root(jar).node(path, null);
    }
}
