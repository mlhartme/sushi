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
import com.jcraft.jsch.Session;
import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;

/**
 * Nodes accessible via sftp.
 *
 * You'll usually have a default identity, protected with a passphrase, available in ssh-agent (either automatically
 * after login or by explicit ssh-add calls).
 *
 * If ssh-agent is not accessible (because the optional jsch-agent dependencies are missing) you have the following
 * - less attractive - options:
 * 1) use an identity that's not protected with a passphrase
 * 2) otherwise:
 *    a) explicitly call addDefaultIdentity with the respective passphrase
 *    b) store the passphrase in ~/.ssh/passphrase
 *
 * Uses Jsch:  http://www.jcraft.com/jsch/
 * See also: http://tools.ietf.org/id/draft-ietf-secsh-filexfer-13.txt
 */
public class SshFilesystem extends Filesystem {
    /** @param trySshAgent disable this if your ssh agent is configured, but you don't want to use it. */
    public static JSch jsch(boolean trySshAgent) throws IOException {
        JSch jsch;

        jsch = new JSch();
        if (trySshAgent) {
            try {
                SshAgent.configure(jsch);
            } catch (NoClassDefFoundError e) {
                // ok -- we have no ssh-agent dependencies
            }
        }
        jsch.setHostKeyRepository(new AcceptAllHostKeyRepository());
        return jsch;
    }

    private int defaultTimeout;
    private final JSch jsch;

    public SshFilesystem(World world, String name, boolean trySshAgent) throws IOException {
        this(world, name, jsch(trySshAgent));
        this.defaultTimeout = 0;
    }

    public SshFilesystem(World world, String name, JSch jsch) {
        super(world, new Features(true, true, true, true, false, false, true), name);

        this.defaultTimeout = 0;
        this.jsch = jsch;
    }

    public Session connect(String host, int port, String user, int timeout) throws JSchException {
        Session session;

        session = jsch.getSession(user, host, port);
        session.connect(timeout);
        return session;
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
        addDefaultIdentityOpt();
        return new SshRoot(this, host, user, timeout);
    }

    //--

    /** adds the default identity if the identity repository is empty */
    public void addDefaultIdentityOpt() throws IOException, JSchException {
        if (jsch.getIdentityNames().isEmpty()) {
            addDefaultIdentity();
        }
    }

    public void addDefaultIdentity() throws IOException, JSchException {
        addDefaultIdentity(null);
    }

    /** @param passphrase null to try to load passphrase from ~/.ssh/passphrase file */
    public void addDefaultIdentity(String passphrase) throws IOException, JSchException {
        Node dir;
        Node file;
        Node key;

        dir = getWorld().getHome().join(".ssh");
        file = dir.join("passphrase");
        if (passphrase == null && file.exists()) {
            passphrase = file.readString().trim();
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
        addIdentity(key, passphrase);
    }

    /**
     * Core method to actually add an identity. Identity is a private/public key pair.
     * Identities are "not interactive" - this method reports missing passphrases or when
     * a passphrase is specified for an identity that does not need one.
     *
     * @param passphrase null of none
     */
    public void addIdentity(Node privateKey, String passphrase) throws IOException, JSchException {
        Identity identity;
        Throwable te;
        Class<?> clz;
        Method m;
        byte[] bytes;

        bytes = privateKey.readBytes();
        // CAUTION: I cannot use
        //   jsch.addIdentity("foo", null, null, null);
        // because in jsch 1.48, there's no way to obtain the resulting identity and the identity.setPassphrase
        // result.
        try {
            clz = Class.forName("com.jcraft.jsch.IdentityFile");
            m = clz.getDeclaredMethod("newInstance", String.class, byte[].class, byte[].class, JSch.class);
            m.setAccessible(true);
            identity = (Identity) m.invoke(null, privateKey.toString(), Arrays.copyOf(bytes, bytes.length), null, jsch);
        } catch (InvocationTargetException e) {
            te = e.getTargetException();
            if (te instanceof JSchException) {
                throw (JSchException) te;
            } else if (te instanceof IOException) {
                throw (IOException) te;
            } else {
                throw new IllegalStateException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException("TODO", e);
        }
        if (passphrase != null) {
            if (!identity.isEncrypted()) {
                throw new JSchException("unexpected passphrase");
            }
            if (!identity.setPassphrase(passphrase.getBytes())) {
                throw new JSchException("invalid passphrase");
            }
        } else {
            if (!identity.setPassphrase(null)) {
                throw new JSchException("missing passphrase");
            }
        }
        jsch.removeIdentity(identity);
        jsch.addIdentity(identity, null);
    }
}
