package net.sf.beezle.sushi.fs;

public class FileNotFoundException extends NodeException {
    public FileNotFoundException(Node node) {
        this(node, "file node found");
    }

    public FileNotFoundException(Node node, String msg) {
        super(node, msg);
    }

    public FileNotFoundException(Node node, Throwable e) {
        this(node);
        initCause(e);
    }
}
