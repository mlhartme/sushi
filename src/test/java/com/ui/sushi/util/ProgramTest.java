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

package de.ui.sushi.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.file.FileNode;
import de.ui.sushi.io.OS;

public class ProgramTest {
    private static final IO IO_OBJ = new IO();

    @Test
    public void normal() throws IOException {
        p("hostname").exec();
    }

    @Test
    public void echo() throws IOException {
        assertEquals("foo", p("echo", "foo").exec().trim());
    }

    @Test
    public void variableSubstitution() throws IOException {
        String var;
        String output;

        var = OS.CURRENT.variable("PATH");
        output = p("echo", var).exec().trim();
        assertTrue(output + " vs " + var, OS.CURRENT != OS.WINDOWS == var.equals(output));
    }

    @Test
    public void noRedirect() throws IOException {
        if (OS.CURRENT != OS.WINDOWS) {
            assertEquals("foo >file\n", p("echo", "foo", ">file").exec());
        } else {
            // TODO
        }
    }

    @Test
    public void env() throws IOException {
        assertTrue(p(environ()).exec().contains("PATH="));
    }

    @Test
    public void myEnv() throws IOException {
        Program p;
        
        p = p(environ());
        p.builder.environment().put("bar", "foo");
        assertTrue(p.exec().contains("bar=foo"));
    }

    @Test
    public void output() throws IOException {
        assertEquals("foo", p("echo", "foo").exec().trim());
    }
    
    @Test
    public void chains() throws IOException {
        assertEquals("foo\nbar", p("bash", "-c", "echo foo && echo bar").exec().trim());
    }

    @Test
    public void noChains() throws IOException {
        assertEquals("foo && echo bar", p("echo", "foo", "&&", "echo", "bar").exec().trim());
    }
    
    private String environ() {
        if (OS.CURRENT == OS.WINDOWS) {
            return "set";
        } else {
            return "env";
        }
    }
    
    public void failure() throws IOException {
        try {
            p("ls", "nosuchfile").exec();
            fail();
        } catch (ExitCode e) {
            // ok
        }
    }

    @Test
    public void notfoundexecFailure() throws IOException {
        try {
            p("nosuchcommand").exec();
            fail();
        } catch (ExitCode e) {
            assertEquals(OS.WINDOWS, OS.CURRENT);
        } catch (IOException e) {
            assertTrue(OS.WINDOWS != OS.CURRENT);
        }
    }
    
    private Program p(String ... args) {
        Program p;
        
        p = new Program((FileNode) IO_OBJ.getHome());
        if (OS.CURRENT == OS.WINDOWS) {
            p.add("cmd", "/C");
        }
        p.add(args);
        return p;
    }
}
