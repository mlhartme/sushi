package de.ui.sushi.fs.ssh;

import de.ui.sushi.fs.Node;

public class Credentials {
    public final Node privateKey;
    public final String passphrase;

    public Credentials(Node privateKey, String passphrase) {
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }
}
