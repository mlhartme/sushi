package net.sf.beezle.sushi.fs;

public class CreateOutputStreamException extends NodeException {
    public CreateOutputStreamException(Node node, Throwable cause) {
        super(node, "cannot create output stream", cause);
    }
}
