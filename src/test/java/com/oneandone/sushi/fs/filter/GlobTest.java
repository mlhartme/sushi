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

package de.ui.sushi.fs.filter;

import java.util.regex.Pattern;

import org.junit.Test;

import static org.junit.Assert.*;

public class GlobTest {
    private Pattern p;
    
    @Test
    public void empty() {
        assertEquals("", Glob.compile("", false));
    }

    @Test
    public void ignoreCase() {
        p = (Pattern) Glob.compile("a", true);
        assertTrue(Glob.matches(p, "a"));
        assertTrue(Glob.matches(p, "A"));
        assertFalse(Glob.matches(p, "b"));
    }

    @Test
    public void suffix() {
        p = (Pattern) Glob.compile("*.java", false);
        assertTrue(Glob.matches(p, "foo.java"));
        assertFalse(Glob.matches(p, "foo.txt"));
        assertTrue(Glob.matches(p, ".java"));
    }

    @Test
    public void all() {
        p = (Pattern) Glob.compile("*.*", true);
        assertFalse(Glob.matches(p, ""));
        assertTrue(Glob.matches(p, "."));
        assertTrue(Glob.matches(p, "foo.bar"));
    }
    
    @Test
    public void x() {
        p = (Pattern) Glob.compile("g.a-*.jar", true);
        assertTrue(Glob.matches(p, "g.a-0.2.jar"));
        assertTrue(Glob.matches(p, "g.A-0.2.jar"));
    }
}

