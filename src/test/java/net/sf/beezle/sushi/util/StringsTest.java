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

import org.junit.Test;

import static org.junit.Assert.*;

public class StringsTest {
    @Test
    public void remove() {
        final String STARTX = "startx";

        assertEquals("start", Strings.removeEnd(STARTX, "x"));
        assertEquals("start", Strings.removeEndOpt(STARTX, "x"));
        assertEquals(STARTX, Strings.removeEndOpt(STARTX, "y"));
        assertEquals("tartx", Strings.removeStart(STARTX, "s"));
        assertEquals("tartx", Strings.removeStartOpt(STARTX, "s"));
        assertSame(STARTX, Strings.removeStartOpt(STARTX, "t"));
    }

    @Test
    public void stripExtension() {
        String f1 = "abc.xml";
        String f2 = ".xml";
        String f3 = "abc";
        String f4 = "abc.def.xml";

        assertTrue("abc".equals(Strings.stripExtension(f1)));
        assertTrue(".xml".equals(Strings.stripExtension(f2)));
        assertTrue("abc".equals(Strings.stripExtension(f3)));
        assertTrue("abc.def".equals(Strings.stripExtension(f4)));
    }

    //--

    @Test
    public void capitalize() {
        assertEquals("", Strings.capitalize(""));
        assertEquals("C", Strings.capitalize("c"));
        assertEquals("Cap", Strings.capitalize("Cap"));
        assertEquals("CaP", Strings.capitalize("caP"));
        assertEquals("1", Strings.capitalize("1"));
    }

    @Test
    public void decapitalize() {
        assertEquals("", Strings.decapitalize(""));
        assertEquals("c", Strings.decapitalize("c"));
        assertEquals("c", Strings.decapitalize("C"));
        assertEquals("caP", Strings.decapitalize("CaP"));
        assertEquals("1", Strings.decapitalize("1"));
    }

    //---

    @Test
    public void blockSimple() {
        assertEquals("", Strings.block("", "", 0, ""));
        assertEquals("foo", Strings.block("", "foo", 10, ""));
        assertEquals("foo bar", Strings.block("", "foo bar", 10, ""));
        assertEquals("barfoo", Strings.block("bar", "foo ", 10, ""));
        assertEquals("foobar", Strings.block("", "foo", 10, "bar"));
    }

    @Test
    public void blockNormalize() {
        assertEquals("foo", Strings.block("", " foo", 10, ""));
        assertEquals("foo", Strings.block("", "foo ", 10, ""));
        assertEquals("foo", Strings.block("", "foo\n", 10, ""));
        assertEquals("foo bar", Strings.block("", "foo  bar", 10, ""));
    }
    @Test
    public void blockBreak() {
        assertEquals("foo-bar-", Strings.block("", "foo bar", 3, "-"));
        assertEquals("foo-bar-", Strings.block("", "foo bar", 4, "-"));
        assertEquals("foo-bar-", Strings.block("", "foo bar", 6, "-"));
        assertEquals("foo bar-", Strings.block("", "foo bar", 7, "-"));
        assertEquals("foo bar-", Strings.block("", "foo  bar", 7, "-"));
    }

    @Test
    public void blockToSmall() {
        assertEquals("foo", Strings.block("", "foo", 0, ""));
        assertEquals("foo-bar-", Strings.block("", "foo bar", 0, "-"));
    }

    //--

    @Test
    public void next1() {
        int[] idx = { 0 };

        assertEquals("", Strings.next("abc", idx, "a", "b"));
        assertEquals("", Strings.next("abc", idx, "a", "b"));
        assertEquals("c", Strings.next("abc", idx, "a", "b"));
        assertEquals(null, Strings.next("abc", idx, "a", "b"));
    }

    @Test
    public void next2() {
        int[] idx = { 0 };

        assertEquals("", Strings.next("abc", idx, "ab", "c"));
        assertEquals("", Strings.next("abc", idx, "ab", "c"));
        assertEquals(null, Strings.next("abc", idx, "ab", "c"));
    }

    @Test
    public void excape() {
        assertEquals("", Strings.escape(""));
        assertEquals("Hello, world", Strings.escape("Hello, world"));
        assertEquals("a\\nb", Strings.escape("a\nb"));
        assertEquals("\\\\\\n\\r\\t", Strings.escape("\\\n\r\t"));
    }
}

