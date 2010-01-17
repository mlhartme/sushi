package de.ui.sushi.fs;

/**
 * To programmatically test the features available in a filesystem. A features is not specific to a node, it's specific
 * to a file system. Features do not change during the lifetime of the vm.
 */
public class Features {
    /** if not, move is emulated by copy and delete */
    public final boolean nativeMove;
    public final boolean atomicMkfile;
    public final boolean atomicMkdir;

    public Features(boolean nativeMove, boolean atomicMkfile, boolean atomicMkdir) {
        this.nativeMove = nativeMove;
        this.atomicMkfile = atomicMkfile;
        this.atomicMkdir = atomicMkdir;
    }
}
