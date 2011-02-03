package de.ui.sushi.fs;

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

    public Features(boolean write, boolean nativeMove, boolean links, boolean modes, boolean atomicMkfile, boolean atomicMkdir) {
        this.write = write;
        this.nativeMove = nativeMove;
        this.links = links;
        this.modes = modes;
        this.atomicMkfile = atomicMkfile;
        this.atomicMkdir = atomicMkdir;
    }
}
