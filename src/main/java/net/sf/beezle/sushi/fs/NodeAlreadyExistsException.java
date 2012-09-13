package net.sf.beezle.sushi.fs;

public class NodeAlreadyExistsException extends NodeException {
    public NodeAlreadyExistsException(Node node) {
        super(node, "node already exists");
    }

    public NodeAlreadyExistsException(Node node, Throwable e) {
        this(node);
        initCause(e);
    }
}
