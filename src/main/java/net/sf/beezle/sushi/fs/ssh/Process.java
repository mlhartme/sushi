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

package net.sf.beezle.sushi.fs.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import net.sf.beezle.sushi.launcher.ExitCode;
import net.sf.beezle.sushi.launcher.Launcher;
import net.sf.beezle.sushi.util.Joiner;

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
        channel.setCommand(Joiner.SPACE.join(command));
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
        return root.getUser() + '@' + root.getHost() + "# " + Joiner.SPACE.join(command);
    }
}
