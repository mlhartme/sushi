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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import net.oneandone.sushi.fs.OnShutdown;
import net.oneandone.sushi.fs.Root;
import net.oneandone.sushi.io.MultiOutputStream;
import net.oneandone.sushi.launcher.ExitCode;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class SshRoot implements Root<SshNode>, Runnable {
    private final SshFilesystem filesystem;

    /** connected session */
    private final Session session;

    // created on demand because it's only needed for nodes, not for "exec" stuff
    private ChannelSftp sftp;

    /** @param password may be null */
    public SshRoot(SshFilesystem filesystem, String host, String user, String password, int timeout) throws JSchException {
        this(filesystem, host, 22, user, password, timeout);
    }

    /** @param password may be null */
    public SshRoot(SshFilesystem filesystem, String host, int port, String user, String password, int timeout) throws JSchException {
        this(filesystem, filesystem.connect(host, port, user, password, timeout));
    }

    public SshRoot(SshFilesystem filesystem, Session session) throws JSchException {
        if (!session.isConnected()) {
            throw new IllegalArgumentException();
        }
        this.filesystem = filesystem;
        this.session = session;
        this.sftp = null;
        OnShutdown.get().onShutdown(this);
    }

    //-- Root interface

    @Override
    public SshFilesystem getFilesystem() {
        return filesystem;
    }

    @Override
    public String getId() {
        return "//" + session.getUserName() + "@" + session.getHost() + "/";
    }

    @Override
    public SshNode node(String path, String encodedQuery) {
        if (encodedQuery != null) {
            throw new IllegalArgumentException(encodedQuery);
        }
        return new SshNode(this, path);
    }

    @Override
    public boolean equals(Object obj) {
        SshRoot root;

        if (obj instanceof SshRoot) {
            root = (SshRoot) obj;
            return getId().equals(root.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return "SshNode host=" + getHost() + ", user=" + getUser();
    }

    //--

    private int allocated = 0;

    public synchronized int getAllocated() {
        return allocated;
    }

    public synchronized ChannelSftp allocateChannelSftp() throws JSchException {
        ChannelSftp result;

        if (sftp != null) {
            result = sftp;
            sftp = null;
        } else {
            result = (ChannelSftp) session.openChannel("sftp");
            result.connect();
        }
        allocated++;
        return result;
    }

    public synchronized void freeChannelSftp(ChannelSftp free) {
        if (sftp == null) {
            sftp = free;
        } else {
            free.disconnect();
        }
        allocated--;
    }

    public ChannelExec createChannelExec() throws JSchException {
        return (ChannelExec) session.openChannel("exec");
    }

    public void close() {
        session.disconnect();
    }

    public Process start(boolean tty, String ... command) throws JSchException {
        return start(tty, MultiOutputStream.createNullStream(), command);
    }

    public Process start(boolean tty, OutputStream out, String ... command) throws JSchException {
        return Process.start(this, tty, out, command);
    }

    //-- exec methods
    //    I'd like to align this with Launcher execution, but I cannot set a working directory in Ssh Execution

    public String exec(String ... command) throws JSchException, ExitCode {
        return exec(true, command);
    }

    public String exec(boolean tty, String ... command) throws JSchException, ExitCode {
        ByteArrayOutputStream out;

        out = new ByteArrayOutputStream();
        try {
            exec(tty, out, command);
        } catch (ExitCode e) {
            throw new ExitCode(e.launcher, e.code, filesystem.getWorld().getSettings().string(out));
        }
        return filesystem.getWorld().getSettings().string(out);
    }

    public void exec(boolean tty, OutputStream out, String ... command) throws JSchException, ExitCode {
        try {
            start(tty, out, command).waitFor();
        } catch (ExitCode e) {
            throw new ExitCode(e.launcher, e.code);
        }
    }

    //--

    public String getUser() {
        return session.getUserName();
    }

    public String getHost() {
        return session.getHost();
    }

    // executes on shutdown
    public void run() {
        close();
    }
}
