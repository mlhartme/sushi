package de.ui.sushi.fs;

/**
 * To programmatically test the features available in a filesystem. A features is not specific to a node, it's specific
 * to a file system. Features do not change during the lifetime of the vm.
 */
public class Features {
    /** can move file under the same root. */
    public final boolean move;
    public final boolean atomicMkfile;
    public final boolean atomicMkdir;

    public Features(boolean move, boolean atomicMkfile, boolean atomicMkdir) {
        this.move = move;
        this.atomicMkfile = atomicMkfile;
        this.atomicMkdir = atomicMkdir;
    }
}
