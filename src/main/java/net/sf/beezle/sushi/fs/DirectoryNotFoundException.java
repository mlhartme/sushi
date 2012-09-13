package net.sf.beezle.sushi.fs;

public class DirectoryNotFoundException extends NodeException {
    public DirectoryNotFoundException(Node node) {
        this(node, "directory not found");
    }

    public DirectoryNotFoundException(Node node, String msg) {
        super(node, msg);
    }

    public DirectoryNotFoundException(Node node, Throwable e) {
        this(node);
        initCause(e);
    }
}
