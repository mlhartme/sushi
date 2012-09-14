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
package net.oneandone.sushi.fs.filter;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

