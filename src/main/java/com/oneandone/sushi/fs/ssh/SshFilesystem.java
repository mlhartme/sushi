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

package com.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.oneandone.sushi.fs.Features;
import com.oneandone.sushi.fs.Filesystem;
import com.oneandone.sushi.fs.World;
import com.oneandone.sushi.fs.NodeInstantiationException;

import java.io.IOException;
import java.net.URI;

/**
 * Nodes accessible via sftp.
 * Uses Jsch:  http://www.jcraft.com/jsch/
 * See also: http://tools.ietf.org/id/draft-ietf-secsh-filexfer-13.txt
 */
public class SshFilesystem extends Filesystem {
    private Credentials defaultCredentials;
    private int defaultTimeout;
    private final JSch jsch;

    public SshFilesystem(World world, String name) {
        super(world, '/', new Features(true, true, true, true, false, false), name);

        // initialized lazily
        defaultCredentials = null;
        defaultTimeout = 0;
        jsch = new JSch();
    }

    public void setDefaultCredentials(Credentials defaultCredentials) {
        this.defaultCredentials = defaultCredentials;
    }

    /** @return never null */
    public Credentials getDefaultCredentials() throws IOException {
        if (defaultCredentials == null) {
            defaultCredentials = Credentials.loadDefault(getWorld());
        }
        return defaultCredentials;
    }

    /** millis */
    public void setDefaultTimeout(int defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    /** millis */
    public int getDefaultTimeout() {
        return defaultTimeout;
    }

    public JSch getJSch() {
        return jsch;
    }

    @Override
    public SshNode node(URI uri, Object extra) throws NodeInstantiationException {
        Credentials credentials;

        if (extra != null) {
            if (extra instanceof Credentials) {
                credentials = (Credentials) extra;
            } else {
                throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
            }
        } else {
            try {
                credentials = getDefaultCredentials();
            } catch (IOException e) {
                throw new NodeInstantiationException(uri, "cannot load credentials", e);
            }
        }
        checkHierarchical(uri);
        try {
            return root(uri.getAuthority(), credentials).node(getCheckedPath(uri), null);
        } catch (JSchException e) {
            throw new NodeInstantiationException(uri, "cannot create root", e);
        } catch (IOException e) {
            throw new NodeInstantiationException(uri, "cannot create root", e);
        }
    }

    public SshRoot localhostRoot() throws JSchException, IOException {
        return root("localhost", getWorld().getWorking().getName(), getDefaultCredentials());
    }

    public SshRoot root(String root, Credentials credentials) throws JSchException {
        return root(root, credentials, defaultTimeout);
    }

    public SshRoot root(String root, Credentials credentials, int timeout) throws JSchException {
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
        return root(host, user, credentials, timeout);
    }

    public SshRoot root(String host, String user) throws JSchException, IOException {
        return root(host, user, getDefaultCredentials());
    }

    public SshRoot root(String host, String user, Credentials credentials) throws JSchException {
        return root(host, user, credentials, defaultTimeout);
    }

    /** @user null to use current user */
    public SshRoot root(String host, String user, Credentials credentials, int timeout) throws JSchException {
        if (credentials == null) {
            throw new IllegalArgumentException();
        }
        if (user == null) {
            user = getWorld().getHome().getName();
        }
        return new SshRoot(this, host, user, credentials, timeout);
    }
}
