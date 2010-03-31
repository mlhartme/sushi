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

package de.ui.sushi.fs.timemachine;

import de.ui.sushi.fs.ExistsException;
import de.ui.sushi.fs.Features;
import de.ui.sushi.fs.Filesystem;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.RootPathException;
import de.ui.sushi.fs.file.FileNode;

import java.io.FileNotFoundException;
import java.net.URI;

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

    public TimeMachineRoot root(String root) throws RootPathException {
        Node dir;

        dir = getIO().node(root);
        if (!(dir instanceof FileNode)) {
            throw new RootPathException("file node expected:" + root);
        }
        try {
            return TimeMachineRoot.create(this, (FileNode) dir);
        } catch (FileNotFoundException e) {
            throw new RootPathException(e);
        } catch (ExistsException e) {
            throw new RootPathException(e);
        }
    }

    public TimeMachineNode node(URI uri) throws RootPathException {
        String path;
        String schemeSpecific;

        checkOpaque(uri);
        schemeSpecific = uri.getSchemeSpecificPart();
        path = after(schemeSpecific, "!");
        if (path == null) {
            throw new RootPathException(uri, "unexpected opaque uri: " + schemeSpecific);
        }
        if (path.endsWith(getSeparator())) {
            throw new RootPathException(uri, "invalid tailing " + getSeparator());
        }
        if (path.startsWith(getSeparator())) {
            throw new RootPathException(uri, "invalid heading " + getSeparator());
        }
        return root(schemeSpecific.substring(0, schemeSpecific.length() - path.length())).node(path);
    }
}
