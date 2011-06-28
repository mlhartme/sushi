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

package net.sf.beezle.sushi.launcher;

import net.sf.beezle.sushi.fs.Settings;
import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.io.OS;
import org.junit.Ignore;
import org.junit.Test;
import sun.rmi.runtime.NewThreadAction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
        ByteArrayOutputStream stdout;
        ByteArrayOutputStream stderr;

        if (OS.CURRENT == OS.WINDOWS) {
            return;
        }
        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        new Launcher((FileNode) WORLD.getWorking(), "bash", "-c", "echo std && echo err 1>&2").exec(stdout, stderr);
        assertEquals("std", new String(stdout.toByteArray()).trim());
        assertEquals("err", new String(stderr.toByteArray()).trim());
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
        ByteArrayOutputStream out;
        ByteArrayInputStream in;

        out = new ByteArrayOutputStream();
        in = new ByteArrayInputStream(str.getBytes(Settings.UTF_8));
        launcher = new Launcher((FileNode) WORLD.getHome(), "cat");
        launcher.exec(out, null, in, false);
        assertEquals(str, new String(out.toByteArray(), Settings.UTF_8));
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
