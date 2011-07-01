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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class StringsTest {
    @Test
    public void indent() {
        assertEquals("  a", Strings.indent("a", "  "));
        assertEquals("  a\n   b", Strings.indent("a\n b", "  "));
    }

    @Test
    public void remove() {
        final String STARTX = "startx";

        assertEquals("start", Strings.removeRight(STARTX, "x"));
        assertEquals("start", Strings.removeRightOpt(STARTX, "x"));
        assertEquals(STARTX, Strings.removeRightOpt(STARTX, "y"));
        assertEquals("tartx", Strings.removeLeft(STARTX, "s"));
        assertEquals("tartx", Strings.removeLeftOpt(STARTX, "s"));
        assertSame(STARTX, Strings.removeLeftOpt(STARTX, "t"));
    }

    @Test
    public void pad() {
        assertEquals("a", Strings.padLeft("a", 0));
        assertEquals("a", Strings.padLeft("a", 1));
        assertEquals(" a", Strings.padLeft("a", 2));
        assertEquals("b", Strings.padRight("b", 0));
        assertEquals("b", Strings.padRight("b", 1));
        assertEquals("b ", Strings.padRight("b", 2));
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

    @Test
    public void excape() {
        assertEquals("", Strings.escape(""));
        assertEquals("Hello, world", Strings.escape("Hello, world"));
        assertEquals("a\\nb", Strings.escape("a\nb"));
        assertEquals("\\\\\\n\\r\\t", Strings.escape("\\\n\r\t"));
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
}

