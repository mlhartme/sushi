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
package net.oneandone.sushi.fs;

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
