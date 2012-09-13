package net.sf.beezle.sushi.fs;

public class NodeNotFoundException extends NodeException {
    public NodeNotFoundException(Node node) {
        super(node, "node not found");
    }

    public NodeNotFoundException(Node node, Throwable e) {
        this(node);
        initCause(e);
    }
}
