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
import de.ui.sushi.fs.NodeInstantiationException;
import de.ui.sushi.fs.file.FileNode;

import java.io.IOException;
import java.net.URI;

/**
 * Nodes accessible via sftp.
 * Uses Jsch:  http://www.jcraft.com/jsch/
 * See also: http://tools.ietf.org/id/draft-ietf-secsh-filexfer-13.txt
 */
public class SshFilesystem extends Filesystem {
    private Credentials credentials;
    private int timeout;
    private JSch jsch;

    public SshFilesystem(IO io, String name) {
        super(io, '/', new Features(true, true, true, true, false, false), name);

        credentials = null;
        timeout = 0;
        jsch = new JSch();
    }

    public void setCredentials(Credentials credentails) {
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
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

    @Override
    public SshNode node(URI uri, Object extra) throws NodeInstantiationException {
        Credentials activeCredentials;

        if (extra != null) {
            if (extra instanceof Credentials) {
                activeCredentials = (Credentials) extra;
            } else {
                throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
            }
        } else {
            activeCredentials = credentials;
        }
        checkHierarchical(uri);
        try {
            return root(uri.getAuthority(), activeCredentials, timeout).node(getCheckedPath(uri));
        } catch (JSchException e) {
            throw new NodeInstantiationException(uri, "cannot create root", e);
        } catch (IOException e) {
            throw new NodeInstantiationException(uri, "cannot create root", e);
        }
    }

    public SshRoot localhostRoot() throws JSchException, IOException {
        return root("localhost", getIO().getWorking().getName(), credentials, timeout);
    }

    public SshRoot root(String root, Credentials activeCredentials) throws JSchException, IOException {
        return root(root, activeCredentials, timeout);
    }

    public SshRoot root(String root, Credentials activeCredentials, int activeTimeout) throws JSchException, IOException {
        int idx;
        String host;
        String user;

        host = root;
        idx = host.indexOf('@');
        if (idx == -1) {
            user = null;
        } else {
            user = host.substring(0, idx);
            host = host.substring(idx + 1);
        }
        return root(host, user, activeCredentials, activeTimeout);
    }

    public SshRoot root(String host, String user, Credentials activeCredentials) throws JSchException, IOException {
        return root(host, user, activeCredentials, timeout);
    }

    /** @user null to use current user */
    public SshRoot root(String host, String user, Credentials activeCredentials, int activeTimeout) throws JSchException, IOException {
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
        if (activeCredentials != null && activeCredentials.passphrase != null) {
            pp = activeCredentials.passphrase;
        } else {
            file = dir.join("passphrase");
            if (file.exists()) {
                pp = file.readString().trim();
            } else {
                pp = "";
            }
        }
        if (activeCredentials != null && activeCredentials.privateKey != null) {
            key = activeCredentials.privateKey;
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
        return new SshRoot(this, host, user, (FileNode) key, pp, activeTimeout);
    }
}
