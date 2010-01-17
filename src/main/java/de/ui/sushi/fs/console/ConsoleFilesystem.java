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

import de.ui.sushi.fs.*;

public class ConsoleFilesystem extends Filesystem implements Root {
    public ConsoleFilesystem(IO io, String name) {
        super(io, '/', new Features(false, false, false, false), name);
    }

    @Override
    public ConsoleFilesystem root(String authority) throws RootPathException {
        if (authority != null) {
            throw new RootPathException("unexpected authority: " + authority);
        }
        return this;
    }

    //-- root methods

    public Filesystem getFilesystem() {
        return this;
    }

    public String getId() {
        return "/";
    }

    public ConsoleNode node(String path) {
        if (path.length() > 0) {
            throw new UnsupportedOperationException();
        }
        return new ConsoleNode(this);
    }
}
