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
import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * See http://www.macosxhints.com/article.php?story=20080623213342356
 *
 * Mount with fstab
 *    dev/sdb2       /media/timemachine hfsplus user,ro,nosuid,nodev,uid=mhm,gid=mhm 0 0
 * does not work, see http://falsepositive.eu/archives/20080307-hfsplus-UIDGID-remapping/21  :(
 */
public class TimeMachineFilesystem extends Filesystem {
    public TimeMachineFilesystem(World world, String name) {
        super(world, new Features(false, false, false, false, false, false, false), name);
    }

    public TimeMachineNode node(URI uri, Object extra) throws NodeInstantiationException {
        String path;
        String schemeSpecific;
        String root;
        Node dir;

        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkOpaque(uri);
        schemeSpecific = uri.getSchemeSpecificPart();
        path = after(schemeSpecific, "!");
        if (path == null) {
            throw new NodeInstantiationException(uri, "missing '!': " + schemeSpecific);
        }
        if (path.endsWith(SEPARATOR_STRING)) {
            throw new NodeInstantiationException(uri, "invalid tailing " + SEPARATOR_STRING);
        }
        if (path.startsWith(SEPARATOR_STRING)) {
            throw new NodeInstantiationException(uri, "invalid heading " + SEPARATOR_STRING);
        }
        root = schemeSpecific.substring(0, schemeSpecific.length() - path.length());
        try {
            dir = getWorld().node(root);
        } catch (URISyntaxException e) {
            throw new NodeInstantiationException(uri, "invalid root '" + root + "'", e);
        }
        if (!(dir instanceof FileNode)) {
            throw new NodeInstantiationException(uri, "file node expected:" + root);
        }
        try {
            return TimeMachineRoot.create(this, (FileNode) dir).node(path, null);
        } catch (DirectoryNotFoundException | ExistsException e) {
            throw new NodeInstantiationException(uri, e.getMessage(), e);
        }
    }
}
