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

package net.sf.beezle.sushi.fs;

/**
 * To programmatically test the features available in a filesystem. A features is not specific to a node, it's specific
 * to a file system. Features do not change during the lifetime of the vm.
 *
 * This is a value object passed to the filesystem constructor (and not an interface, e.g. implemented by Filesystems)
 * to ensure that values won't change.
 */
public class Features {
    public final boolean write;

    /** if not, move is emulated by copy and delete */
    public final boolean nativeMove;

    /** if not, link methods throw an UnsupportedOperationException */
    public final boolean links;

    public final boolean modes;

    public final boolean atomicMkfile;
    public final boolean atomicMkdir;

    /** true, when writeTo is more efficient than createInputStream */
    public final boolean inverseIO;

    public Features(boolean write, boolean nativeMove, boolean links, boolean modes, boolean atomicMkfile,
                    boolean atomicMkdir, boolean inverseIO) {
        this.write = write;
        this.nativeMove = nativeMove;
        this.links = links;
        this.modes = modes;
        this.atomicMkfile = atomicMkfile;
        this.atomicMkdir = atomicMkdir;
        this.inverseIO = inverseIO;
    }
}
