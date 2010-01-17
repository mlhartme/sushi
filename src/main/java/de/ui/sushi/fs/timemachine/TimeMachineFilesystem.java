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

import java.io.IOException;

import de.ui.sushi.fs.*;
import de.ui.sushi.fs.file.FileNode;

/**
 * See http://www.macosxhints.com/article.php?story=20080623213342356
 *
 * Mount with fstab
 *    dev/sdb2       /media/timemachine hfsplus user,ro,nosuid,nodev,uid=mhm,gid=mhm 0 0
 * does not work, see http://falsepositive.eu/archives/20080307-hfsplus-UIDGID-remapping/21  :(
 */
public class TimeMachineFilesystem extends Filesystem {
    public TimeMachineFilesystem(IO io, String name) {
        super(io, '/', new Features(false, false, false, false), name);
    }

    @Override
    public String opaquePath(String schemeSpecific) throws RootPathException {
        return after(schemeSpecific, "!");
    }

    @Override
    public TimeMachineRoot root(String root) throws RootPathException {
        Node dir;

        dir = getIO().node(root);
        if (!(dir instanceof FileNode)) {
            throw new RootPathException("file node expected:" + root);
        }
        try {
            return TimeMachineRoot.create(this, (FileNode) dir);
        } catch (IOException e) {
            throw new RootPathException(e);
        }
    }
}
