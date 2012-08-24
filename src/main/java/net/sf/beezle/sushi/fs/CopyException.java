/**
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
package net.sf.beezle.sushi.fs;

public class CopyException extends NodeException {
    public final Node dest;

    public CopyException(Node src, Node dest, Throwable e) {
        this(src, dest, e.getMessage(), e);
    }

    public CopyException(Node src, Node dest, String msg, Throwable e) {
        super(src, "copy failed: " + dest + ": " + msg);
        this.dest = dest;
        initCause(e);
    }
}
