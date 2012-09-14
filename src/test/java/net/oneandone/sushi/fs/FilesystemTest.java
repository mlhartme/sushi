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
package net.oneandone.sushi.fs;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class FilesystemTest {
    private final Filesystem fs = new Filesystem(new World(), null, "foo") {
        @Override
        public Node node(URI uri, Object extra) throws NodeInstantiationException {
            return null;
        }
    };

    @Test
    public void normalizeUnchanged() {
        check("");
        check("/");
        check("/foo");
        check("/bar/");
        check("abc");
        check("x/y/z");
        check("nix.");
        check("/.com");
        check("foo./");
        check("foo.bar");
        check("some/path/foo.bar");
    }

    @Test
    public void normalizeDoubleSlash() {
        check("/", "//");
        check("/", "///");
        check("/", "////");
        check("A/B", "A//B");
        check("A/B", "A///B");
        check("1/2/3/", "1///2///3////");
    }

    @Test
    public void normalizeDots() {
        check("", ".");
        check("a", "a/.");
        check("A", "./A");
        check("a/b", "a/./b");
    }

    @Test
    public void normalizeDoubleDots() {
        check("", "a/..");
        check("", "a/../");
        check("b", "a/../b");
        check("b/", "a/../b/");
        check("/b", "/a/../b");
        check("A/B", "a/b/../../A/B");
        check("a/x/Z", "a/b/../x/y/z/../../Z");
    }

    private void check(String unchanged) {
        check(unchanged, unchanged);
    }

    private void check(String expected, String path) {
        StringBuilder builder;

        builder = new StringBuilder(path);
        fs.normalize(builder);
        assertEquals(expected, builder.toString());
    }
}
