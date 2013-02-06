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
package net.oneandone.sushi.launcher;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.OS;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LauncherTest {
    private static final World WORLD = new World();

    @Test
    public void normal() throws Failure {
        launch("hostname").exec();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void noCommand() throws Failure {
        launch().exec();
    }

    @Test(expected = IllegalStateException.class)
    public void noDirectory() throws Failure {
        new Launcher("hostname").exec();
    }

    @Test
    public void echo() throws Failure {
        assertEquals("foo", launch("echo", "foo").exec().trim());
    }

    @Test
    public void variableSubstitution() throws Failure {
        String var;
        String output;

        var = OS.CURRENT.variable("PATH");
        output = launch("echo", var).exec().trim();
        assertTrue(output + " vs " + var, OS.CURRENT != OS.WINDOWS == var.equals(output));
    }

    @Test
    public void noRedirect() throws Failure {
        if (OS.CURRENT != OS.WINDOWS) {
            assertEquals("foo >file\n", launch("echo", "foo", ">file").exec());
        } else {
            // TODO
        }
    }

    @Test
    public void env() throws Failure {
        assertTrue(launch(environ()).exec().contains("PATH="));
    }

    @Test
    public void myEnv() throws Failure {
        Launcher p;

        p = launch(environ());
        p.env("bar", "foo");
        assertTrue(p.exec().contains("bar=foo"));
    }

    @Test
    public void stdout() throws Failure {
        assertEquals("foo", launch("echo", "foo").exec().trim());
    }

    @Test
    public void stderr() throws Failure {
        if (OS.CURRENT == OS.WINDOWS) {
            return;
        }
        assertEquals("err", launch("bash", "-c", "echo err 1>&2").exec().trim());
    }

    @Test
    public void stdstreams() throws Failure {
        StringWriter stdout;
        StringWriter stderr;

        if (OS.CURRENT == OS.WINDOWS) {
            return;
        }
        stdout = new StringWriter();
        stderr = new StringWriter();
        new Launcher((FileNode) WORLD.getWorking(), "bash", "-c", "echo std && echo err 1>&2").exec(stdout, stderr);
        assertEquals("std", stdout.toString().trim());
        assertEquals("err", stderr.toString().trim());
    }

    @Test
    public void chains() throws Failure {
    	if (OS.CURRENT == OS.WINDOWS) {
    		return;
    	}
        assertEquals("foo\nbar", launch("bash", "-c", "echo foo && echo bar").exec().trim());
    }

    @Test
    public void noChains() throws Failure {
        assertEquals(OS.CURRENT == OS.WINDOWS ? "foo \r\nbar" : "foo && echo bar",
        		launch("echo", "foo", "&&", "echo", "bar").exec().trim());
    }

    private String environ() {
        if (OS.CURRENT == OS.WINDOWS) {
            return "set";
        } else {
            return "env";
        }
    }

    @Test
    public void stdin() throws Failure, UnsupportedEncodingException {
        stdin("");
        stdin("hello");
        stdin("foo\nbar\n");
    }

    private void stdin(String str) throws Failure, UnsupportedEncodingException {
        Launcher launcher;
        StringWriter out;
        StringReader in;

        out = new StringWriter();
        in = new StringReader(str);
        launcher = new Launcher((FileNode) WORLD.getHome(), "cat");
        launcher.exec(out, null, false, in, false);
        assertEquals(str, out.toString());
    }

    @Test
    public void failure() throws Failure {
        try {
            launch("ls", "nosuchfile").exec();
            fail();
        } catch (ExitCode e) {
            // ok
        }
    }

    @Test
    public void notfoundexecFailure() {
        try {
            launch("nosuchcommand").exec();
            fail();
        } catch (ExitCode e) {
            assertEquals(OS.WINDOWS, OS.CURRENT);
        } catch (Failure e) {
            assertTrue(OS.WINDOWS != OS.CURRENT);
        }
    }

    private Launcher launch(String... args) {
        Launcher launcher;

        launcher = new Launcher((FileNode) WORLD.getHome());
        if (OS.CURRENT == OS.WINDOWS) {
            launcher.arg("cmd", "/C");
        }
        launcher.arg(args);
        return launcher;
    }
}
