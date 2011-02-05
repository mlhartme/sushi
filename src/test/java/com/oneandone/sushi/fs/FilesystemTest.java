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

package com.oneandone.sushi.fs;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/** Note: IO.node methods are tested in NodeTest. */
public class FilesystemTest {
    private Filesystem fs = new Filesystem(new IO(), '/', null, "foo") {
        @Override
        public Node node(URI uri, Object extra) throws NodeInstantiationException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
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
