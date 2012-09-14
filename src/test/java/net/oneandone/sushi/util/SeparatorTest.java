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
        check(Separator.RAW_LINE.split("1abc\r2\n3\r\n  4\n\r"), "1abc\r", "2\n", "3\r\n", "  4\n\r");
    }

    //--

    private void check(List<String> actual, String ... expected) {
        assertEquals(java.util.Arrays.asList(expected), actual);
    }
}
