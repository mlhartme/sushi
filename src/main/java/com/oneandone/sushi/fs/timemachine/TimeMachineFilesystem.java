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
import com.oneandone.sushi.fs.Features;
import com.oneandone.sushi.fs.Filesystem;
import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.fs.Node;
import com.oneandone.sushi.fs.NodeInstantiationException;
import com.oneandone.sushi.fs.file.FileNode;

import java.io.FileNotFoundException;
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
    public TimeMachineFilesystem(IO io, String name) {
        super(io, '/', new Features(false, false, false, false, false, false), name);
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
        if (path.endsWith(getSeparator())) {
            throw new NodeInstantiationException(uri, "invalid tailing " + getSeparator());
        }
        if (path.startsWith(getSeparator())) {
            throw new NodeInstantiationException(uri, "invalid heading " + getSeparator());
        }
        root = schemeSpecific.substring(0, schemeSpecific.length() - path.length());
        try {
            dir = getIO().node(root);
        } catch (URISyntaxException e) {
            throw new NodeInstantiationException(uri, "invalid root '" + root + "'", e);
        }
        if (!(dir instanceof FileNode)) {
            throw new NodeInstantiationException(uri, "file node expected:" + root);
        }
        try {
            return TimeMachineRoot.create(this, (FileNode) dir).node(path, null);
        } catch (FileNotFoundException e) {
            throw new NodeInstantiationException(uri, e.getMessage(), e);
        } catch (ExistsException e) {
            throw new NodeInstantiationException(uri, e.getMessage(), e);
        }
    }
}
