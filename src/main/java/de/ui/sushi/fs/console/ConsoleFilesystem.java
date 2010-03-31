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

package de.ui.sushi.fs.console;

import de.ui.sushi.fs.Features;
import de.ui.sushi.fs.Filesystem;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.Root;
import de.ui.sushi.fs.RootPathException;

import java.net.URI;

public class ConsoleFilesystem extends Filesystem implements Root {
    public ConsoleFilesystem(IO io, String name) {
        super(io, '/', new Features(true, false, false, false, false, false), name);
    }

    public Filesystem getFilesystem() {
        return this;
    }

    public String getId() {
        return "/";
    }

    // TODO
    @Override
    public ConsoleNode node(String path) {
        return new ConsoleNode(this);
    }

    public ConsoleNode node(URI uri) throws RootPathException {
        checkHierarchical(uri);
        if (!getSeparator().equals(uri.getPath())) {
            throw new RootPathException(uri, "unexpected path");
        }
        return new ConsoleNode(this);
    }
}
