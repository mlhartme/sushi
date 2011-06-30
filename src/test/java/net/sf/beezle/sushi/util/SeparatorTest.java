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

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SeparatorTest {
    @Test
    public void joinEmpty() {
        assertEquals("", Separator.on(" ").join(new Object[] {}));
    }

    @Test
    public void joinNormal() {
        assertEquals("1 2", Separator.on(' ').join(1, 2));
    }

    @Test
    public void joinSkipNulls() {
        assertEquals("1,3", Separator.on(',').skipNull().join(1, null, 3));
    }

    @Test
    public void joinForNull() {
        assertEquals("null, null, 3", Separator.on(", ").forNull("null").join(null, null, 3));
    }

    @Test
    public void joinUseForNullSkip() {
        assertEquals("null, null, 3", Separator.on(", ").skipNull().forNull("null").join(null, null, 3));
    }

    @Test
    public void joinTrim() {
        assertEquals("a\nb\nc\n", Separator.on('\n').trim().join(" a", "b", "c ", " "));
    }

    @Test
    public void joinSkipEmpty() {
        assertEquals("1,3", Separator.on(',').skipEmpty().trim().forNull("").join(1, "", null, " ", 3));
    }

    //--

    @Test(expected=IllegalArgumentException.class)
    public void splitEmpty() {
        Separator.on("");
    }

    @Test
    public void splitNormal() {
        check(Separator.on(" ").split(""));
        check(Separator.on(" ").split("1"), "1");
        check(Separator.on(" ").split("a b"), "a", "b");
    }
    @Test
    public void splitPlus() {
        check(Separator.on("+").split("++"), "", "", "");
    }

    @Test
    public void splitTrim() {
        check(Separator.on(",").trim().split("a, b,c , d ,"), "a", "b", "c", "d", "");
    }

    @Test
    public void splitSkipEmpty() {
        check(Separator.on(",").trim().skipEmpty().split("a,, ,"), "a");
    }

    @Test
    public void splitWhitespace() {
        check(Separator.SPACE.split(""));
        check(Separator.SPACE.split(" a"), "a");
        check(Separator.SPACE.split("a b  c\td\n e "), "a", "b", "c", "d", "e");
    }

    @Test
    public void splitLines() {
        check(Separator.LINE.split("1\r2\n3\r\n4"), "1", "2", "3", "4");
    }

    //--

    private void check(List<String> actual, String ... expected) {
        assertEquals(java.util.Arrays.asList(expected), actual);
    }
}
