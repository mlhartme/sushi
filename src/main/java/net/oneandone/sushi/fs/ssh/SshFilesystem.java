/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.net.URI;

/**
 * Nodes accessible via sftp.
 * Uses Jsch:  http://www.jcraft.com/jsch/
 * See also: http://tools.ietf.org/id/draft-ietf-secsh-filexfer-13.txt
 */
public class SshFilesystem extends Filesystem {
    private int defaultTimeout;
    private final JSch jsch;

    public SshFilesystem(World world, String name) {
        super(world, new Features(true, true, true, true, false, false, true), name);

        defaultTimeout = 0;
        jsch = new JSch();
        jsch.setHostKeyRepository(new AcceptAllHostKeyRepository());
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
        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkHierarchical(uri);
        try {
            return root(uri.getAuthority()).node(getCheckedPath(uri), null);
        } catch (JSchException | IOException e) {
            throw new NodeInstantiationException(uri, "cannot create root", e);
        }
    }

    public SshRoot localhostRoot() throws JSchException, IOException {
        return root("localhost", getWorld().getWorking().getName());
    }

    public SshRoot root(String root) throws JSchException, IOException {
        return root(root, defaultTimeout);
    }

    public SshRoot root(String root, int timeout) throws JSchException, IOException {
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
        return root(host, user, timeout);
    }

    public SshRoot root(String host, String user) throws JSchException, IOException {
        return root(host, user, defaultTimeout);
    }

    /** @param user null to use current user */
    public SshRoot root(String host, String user, int timeout) throws JSchException, IOException {
        if (user == null) {
            user = getWorld().getHome().getName();
        }
        if (jsch.getIdentityNames().isEmpty()) {
            jsch.addIdentity(SshKey.loadDefault(getWorld(), jsch), null);
        }
        return new SshRoot(this, host, user, timeout);
    }
}
