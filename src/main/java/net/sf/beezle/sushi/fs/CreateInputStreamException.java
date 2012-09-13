package net.sf.beezle.sushi.fs;

public class CreateInputStreamException extends NodeException {
    public CreateInputStreamException(Node node, Throwable cause) {
        super(node, "createInputStream failed", cause);
    }
}
