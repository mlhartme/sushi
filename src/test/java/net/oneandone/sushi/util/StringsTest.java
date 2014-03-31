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
package net.oneandone.sushi.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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

