/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.ui.sushi.fs.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import de.ui.sushi.fs.Features;
import de.ui.sushi.fs.Filesystem;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.RootPathException;
import de.ui.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URI;

/**
 * Nodes accessible via sftp.
 * Uses Jsch:  http://www.jcraft.com/jsch/
 * See also: http://tools.ietf.org/id/draft-ietf-secsh-filexfer-13.txt
 */
public class SshFilesystem extends Filesystem {
    private Node privateKey;
    private String passphrase;
    private int timeout;
    private JSch jsch;

    public SshFilesystem(IO io, String name) {
        super(io, '/', new Features(true, true, true, true, false, false), name);

        privateKey = null;
        passphrase = null;
        timeout = 0;
        jsch = new JSch();
    }

    public void setPrivateKey(Node privateKey) {
        this.privateKey = privateKey;
    }

    public Node getPrivateKey() {
        return privateKey;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public String getPassphrase() {
        return passphrase;
    }

    /** millis */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /** millis */
    public int getTimeout() {
        return timeout;
    }

    public JSch getJSch() {
        return jsch;
    }

    public SshRoot root(String root) throws RootPathException {
        int idx;
        String host;
        String user;

        host = root;
        idx = host.indexOf('@');
        try {
            if (idx == -1) {
                user = null;
            } else {
                user = host.substring(0, idx);
                host = host.substring(idx + 1);
            }
            try {
                return root(host, user);
            } catch (JSchException e) {
                throw new RootPathException(e);
            }
        } catch (IOException e) {
            throw new RootPathException(e);
        }
    }

    public SshRoot localRoot() throws JSchException, IOException {
        return root("localhost", getIO().getWorking().getName());
    }

    /** @user null to use current user */
    public SshRoot root(String host, String user) throws JSchException, IOException {
        IO io;
        Node dir;
        Node file;
        Node key;
        String pp;

        io = getIO();
        if (user == null) {
            user = io.getHome().getName();
        }
        dir = io.getHome().join(".ssh");
        if (passphrase != null) {
            pp = passphrase;
        } else {
            file = dir.join("passphrase");
            if (file.exists()) {
                pp = file.readString().trim();
            } else {
                pp = "";
            }
        }
        if (privateKey != null) {
            key = privateKey;
        } else {
            key = dir.join("id_dsa");
            if (!key.exists()) {
                key = dir.join("id_rsa");
                if (!key.exists()) {
                    key = dir.join("identity");
                }
            }
        }
        if (!key.isFile()) {
            throw new IOException("private key not found: " + key);
        }
        if (!(key instanceof FileNode)) {
            // TODO: what about security?
            key = key.copyFile(io.getTemp().createTempFile());
        }
        return new SshRoot(this, host, user, (FileNode) key, pp, timeout);
    }

    public SshNode node(URI uri) throws RootPathException {
        checkHierarchical(uri);
        return root(uri.getAuthority()).node(getCheckedPath(uri));
    }
}
