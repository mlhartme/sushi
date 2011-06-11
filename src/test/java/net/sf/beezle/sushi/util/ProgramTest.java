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

package net.sf.beezle.sushi.util;

import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.io.OS;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProgramTest {
    private static final World WORLD = new World();

    @Test
    public void normal() throws ProgramException {
        p("hostname").exec();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void noCommand() throws ProgramException {
        p().exec();
    }

    @Test(expected = IllegalStateException.class)
    public void noDirectory() throws ProgramException {
        new Program("hostname").exec();
    }

    @Test
    public void echo() throws ProgramException {
        assertEquals("foo", p("echo", "foo").exec().trim());
    }

    @Test
    public void variableSubstitution() throws ProgramException {
        String var;
        String output;

        var = OS.CURRENT.variable("PATH");
        output = p("echo", var).exec().trim();
        assertTrue(output + " vs " + var, OS.CURRENT != OS.WINDOWS == var.equals(output));
    }

    @Test
    public void noRedirect() throws ProgramException {
        if (OS.CURRENT != OS.WINDOWS) {
            assertEquals("foo >file\n", p("echo", "foo", ">file").exec());
        } else {
            // TODO
        }
    }

    @Test
    public void env() throws ProgramException {
        assertTrue(p(environ()).exec().contains("PATH="));
    }

    @Test
    public void myEnv() throws ProgramException {
        Program p;

        p = p(environ());
        p.env("bar", "foo");
        assertTrue(p.exec().contains("bar=foo"));
    }

    @Test
    public void output() throws ProgramException {
        assertEquals("foo", p("echo", "foo").exec().trim());
    }

    @Test
    public void chains() throws ProgramException {
    	if (OS.CURRENT == OS.WINDOWS) {
    		return;
    	}
        assertEquals("foo\nbar", p("bash", "-c", "echo foo && echo bar").exec().trim());
    }

    @Test
    public void noChains() throws ProgramException {
        assertEquals(OS.CURRENT == OS.WINDOWS ? "foo \r\nbar" : "foo && echo bar",
        		p("echo", "foo", "&&", "echo", "bar").exec().trim());
    }

    private String environ() {
        if (OS.CURRENT == OS.WINDOWS) {
            return "set";
        } else {
            return "env";
        }
    }

    public void failure() throws ProgramException {
        try {
            p("ls", "nosuchfile").exec();
            fail();
        } catch (ExitCode e) {
            // ok
        }
    }

    @Test
    public void notfoundexecFailure() {
        try {
            p("nosuchcommand").exec();
            fail();
        } catch (ExitCode e) {
            assertEquals(OS.WINDOWS, OS.CURRENT);
        } catch (ProgramException e) {
            assertTrue(OS.WINDOWS != OS.CURRENT);
        }
    }

    private Program p(String ... args) {
        Program p;

        p = new Program((FileNode) WORLD.getHome());
        if (OS.CURRENT == OS.WINDOWS) {
            p.arg("cmd", "/C");
        }
        p.arg(args);
        return p;
    }
}
