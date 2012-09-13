package net.sf.beezle.sushi.fs;

public class ModeException extends NodeException {
    public ModeException(Node node, Throwable cause) {
        super(node, "mode failure", cause);
    }

    public ModeException(Node node, String msg) {
        super(node, msg);
    }
}