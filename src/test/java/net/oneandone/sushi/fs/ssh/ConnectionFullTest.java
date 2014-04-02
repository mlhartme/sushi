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

import com.jcraft.jsch.JSchException;
import net.oneandone.sushi.TestProperties;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.launcher.ExitCode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConnectionFullTest {
    private static final World WORLD = new World();

    public static SshRoot open() throws JSchException, IOException {
        return WORLD.getFilesystem("ssh", SshFilesystem.class).root(host(), user(), null);
    }

    public static String host() throws JSchException, IOException {
        String host;
        InetAddress addr;

        host = TestProperties.get("ssh.host");
        if (host == null) {
            try {
                addr = InetAddress.getLocalHost();
                host = addr.getHostName();
            } catch (UnknownHostException e) {
                host = "localhost";
            }
        }
        return host;
    }

    public static String user() throws JSchException, IOException {
        String user;

        user = TestProperties.get("ssh.user");
        if (user.isEmpty()) {
            user = System.getProperty("user.name");
        }
        return user;
    }


    private SshRoot root;

    @Before
    public void setUp() throws Exception {
        root = open();
    }

    @After
    public void tearDown() {
        if (root != null) {
            root.close();
        }
    }

    @Test
    public void normal() throws Exception {
        assertEquals("\r\n", root.exec("echo"));
        assertEquals("hello\r\n", root.exec("echo", "hello"));
        assertEquals("again\r\n", root.exec("echo", "again"));
        assertEquals("err\r\n", root.exec("echo", "err", "1>&2"));
        try {
            root.exec("commandnotfound");
            fail();
        } catch (ExitCode e) {
            assertTrue(e.output.contains("commandnotfound"));
        }

        assertEquals("alive\r\n", root.exec("echo", "alive"));
    }

    @Test
    public void script() throws Exception {
        assertEquals("6", root.exec(
                "RESULT=0;\n" +
                "for N in 1 2 3; do\n" +
                "  let RESULT=$RESULT+$N;\n" +
                "done;\n" +
                "echo $RESULT;").trim());
    }

    @Test
    public void variablesLost() throws Exception {
        assertEquals("\r\n", root.exec("echo", "$FOO"));
        root.exec("export", "FOO=bar");
        assertEquals("\r\n", root.exec("echo", "$FOO"));
    }

    @Test
    public void directoryLost() throws Exception {
        String start;

        start = root.exec("pwd");
        assertEquals("/usr\r\n", root.exec("cd", "/usr", "&&", "pwd"));
        assertEquals(start, root.exec("pwd"));
    }

    @Test
    public void shell() throws Exception {
        root.exec("echo", "-e", "\\003320l");

        assertEquals("", root.exec("exit", "0", "||", "echo", "dontprintthis"));
        assertEquals("a\r\nb\r\n", root.exec("echo", "a", "&&", "echo", "b"));
        assertEquals("a\r\n", root.exec("echo", "a", "||", "echo", "b"));
        assertEquals("file\r\n", root.exec(
                "if", "test", "-a", "/etc/profile;",
                "then", "echo", "file;",
                "else", "echo", "nofile;", "fi"));
        assertEquals("nofile\r\n", root.exec(
                "if", "test", "-a", "nosuchfile;",
                "then", "echo", "file;",
                "else", "echo", "nofile;", "fi"));
    }

    @Test
    public void timeout() throws Exception {
        Process process;

        process = root.start(true, "sleep", "5");
        try {
            process.waitFor(1000);
            fail();
        } catch (TimeoutException e) {
            // ok
        }
    }

    @Test
    public void longline() throws Exception {
        String longline = "1234567890" +
            "1234567890" +
            "1234567890" +
            "1234567890" +
            "1234567890" +
            "1234567890" +
            "1234567890" +
            "1234567890" +
            "1234567890" +
            "1234567890";
        assertEquals(longline + "\r\n", root.exec("echo", longline));
    }

    @Test
    public void cancel() throws Exception {
        String tmp = "/tmp/cancel-sushi";
        String msg;

        root.exec("rm", "-f", tmp);
        root.start(true, "sleep", "2", "&&", "echo", "hi", ">" + tmp);
        Thread.sleep(500);
        tearDown();
        Thread.sleep(3000);
        setUp();
        try {
            root.exec("LANG=us", "ls", tmp);
            fail();
        } catch (ExitCode e) {
            msg = e.getMessage();
            assertTrue(msg, msg.contains("No such file"));
        }
    }

    @Test
    public void erroroutput() throws Exception {
        try {
            root.exec("echo", "foo", "&&", "exit", "1");
            fail();
        } catch (ExitCode e) {
            assertEquals(1, e.code);
            assertEquals("foo\r\n", e.output);
        }
    }

    @Test
    public void duration() throws Exception {
        Process process;

        process = root.start(true, "sleep", "2");
        process.waitFor();
        assertTrue(process.duration() >= 2000);
        assertTrue(process.duration() <= 2600);
    }

    @Test
    public void correctPassphrase() throws Exception {
        identity("with-passphrase", "passphrase");
    }

    @Test
    public void invalidPassphrase() throws Exception {
        try {
            identity("with-passphrase", "invalid");
            fail();
        } catch (JSchException e) {
            assertEquals("invalid passphrase", e.getMessage());
        }
    }

    @Test
    public void noPassphrase() throws Exception {
        identity("without-passphrase", null);
    }

    @Test
    public void unexpectedPassphrase() throws Exception {
        try {
            identity("without-passphrase", "invalid");
            fail();
        } catch (JSchException e) {
            assertEquals("unexpected passphrase", e.getMessage());
        }
    }

    private World identity(String name, String passphrase) throws Exception {
        World world;
        SshFilesystem fs;

        world = new World();
        fs = world.getFilesystem("ssh", SshFilesystem.class);
        fs.addIdentity(world.guessProjectHome(getClass()).join("src/test/ssh/" + name + "/id_rsa"), passphrase);
        return world;
    }

    @Test
    public void multiplIdentities() throws JSchException, IOException {
        World world;
        SshFilesystem fs;

        world = new World();
        fs = world.getFilesystem("ssh", SshFilesystem.class);
        fs.addDefaultIdentityOpt();
        fs.addIdentity(world.guessProjectHome(getClass()).join("src/test/ssh/without-passphrase/id_rsa"), null);
        fs.addIdentity(world.guessProjectHome(getClass()).join("src/test/ssh/with-passphrase/id_rsa"), "passphrase");
        fs.root(host(), user(), null).exec("ls");
    }
}
