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

package net.sf.beezle.sushi.fs.console;

import net.sf.beezle.sushi.fs.Features;
import net.sf.beezle.sushi.fs.Filesystem;
import net.sf.beezle.sushi.fs.NodeInstantiationException;
import net.sf.beezle.sushi.fs.Root;
import net.sf.beezle.sushi.fs.World;

import java.net.URI;

public class ConsoleFilesystem extends Filesystem implements Root<ConsoleNode> {
    public ConsoleFilesystem(World world, String name) {
        super(world, '/', new Features(true, false, false, false, false, false), name);
    }

    public Filesystem getFilesystem() {
        return this;
    }

    public String getId() {
        return "/";
    }

    // TODO
    @Override
    public ConsoleNode node(String path, String encodedQuery) {
        return new ConsoleNode(this);
    }

    @Override
    public ConsoleNode node(URI uri, Object extra) throws NodeInstantiationException {
        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkHierarchical(uri);
        if (!URI_SEPARATOR.equals(uri.getPath())) {
            throw new NodeInstantiationException(uri, "unexpected path");
        }
        return new ConsoleNode(this);
    }
}
