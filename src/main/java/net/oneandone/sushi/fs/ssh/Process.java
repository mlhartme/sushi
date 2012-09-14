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
import com.jcraft.jsch.JSchException;
import net.oneandone.sushi.launcher.ExitCode;
import net.oneandone.sushi.launcher.Launcher;
import net.oneandone.sushi.util.Separator;

import java.io.OutputStream;

/** Process on the remote host */
public class Process {
    public static Process start(SshRoot root, boolean tty, OutputStream out, String ... command)
    throws JSchException {
        TimedOutputStream dest;
        ChannelExec channel;

        dest = new TimedOutputStream(out);
        channel = root.createChannelExec();
        // tty=true propagates ctrl-c to the remote host:
        // (unfortunately, this causes ssh servers to send cr/lf, and I didn't find
        // a way to stop this - I tried setPtyType and setTerminalMode)
        channel.setPty(tty);
        // TODO: http://tools.ietf.org/html/rfc4250
        // TODO: channel.setTerminalMode(new byte[] { 70, 0, 0, 0, 0, /*71, 0, 0, 0, 0,*/ 0 });
        channel.setCommand(Separator.SPACE.join(command));
        channel.setInputStream(null);
        channel.setOutputStream(dest);
        channel.setExtOutputStream(dest);
        channel.connect();
        return new Process(root, command, channel, dest);
    }

    private final SshRoot root;
    private final String[] command;
    private final TimedOutputStream dest;
    private final ChannelExec channel;

    public Process(SshRoot root, String[] command, ChannelExec channel, TimedOutputStream dest) {
        if (channel == null) {
            throw new IllegalArgumentException();
        }
        this.root = root;
        this.command = command;
        this.channel = channel;
        this.dest = dest;
    }

    public SshRoot getRoot() {
        return root;
    }

    public boolean isTerminated() {
        return channel.isClosed();
    }

    public void waitFor() throws JSchException, ExitCode {
        try {
            waitFor(1000L * 60 * 60 * 24); // 1 day
        } catch (InterruptedException e) {
            throw new RuntimeException("unexpected", e);
        }
    }

    /**
     * Waits for termination.
     *
     * @param timeout CAUTION: &lt;= 0 immediately times out */
    public void waitFor(long timeout) throws JSchException, ExitCode, InterruptedException {
        long deadline;

        try {
            deadline = System.currentTimeMillis() + timeout;
            while (!channel.isClosed()) {
                if (System.currentTimeMillis() >= deadline) {
                    throw new TimeoutException(this);
                }
                Thread.sleep(100); // throws InterruptedException
            }
            if (channel.getExitStatus() != 0) {
                throw new ExitCode(new Launcher(command), channel.getExitStatus());
            }
        } finally {
            channel.disconnect();
        }
    }

    public long duration() {
        return dest.duration;
    }

    @Override
    public String toString() {
        return root.getUser() + '@' + root.getHost() + "# " + Separator.SPACE.join(command);
    }
}
