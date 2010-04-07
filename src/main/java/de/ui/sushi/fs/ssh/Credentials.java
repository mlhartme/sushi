package de.ui.sushi.fs.ssh;

import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.file.FileNode;

import java.io.IOException;

/** Private key with passphrase */
public class Credentials {
    public static Credentials loadDefault(IO io) throws IOException {
        Node dir;
        Node file;
        Node key;
        String passphrase;

        dir = io.getHome().join(".ssh");
        file = dir.join("passphrase");
        if (file.exists()) {
            passphrase = file.readString().trim();
        } else {
            passphrase = "";
        }
        key = dir.join("id_dsa");
        if (!key.exists()) {
            key = dir.join("id_rsa");
            if (!key.exists()) {
                key = dir.join("identity");
            }
        }
        if (!key.isFile()) {
            throw new IOException("private key not found: " + key);
        }
        if (!(key instanceof FileNode)) {
            // TODO: what about security?
            key = key.copyFile(io.getTemp().createTempFile());
        }
        return new Credentials((FileNode) key, passphrase);
    }

    public final FileNode privateKey;
    public final String passphrase;

    public Credentials(FileNode privateKey) {
        this(privateKey, "");
    }

    public Credentials(FileNode privateKey, String passphrase) {
        if (passphrase == null) {
            throw new IllegalArgumentException();
        }
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }
}
