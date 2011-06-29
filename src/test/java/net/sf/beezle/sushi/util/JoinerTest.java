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

public class JoinerTest {
    @Test
    public void empty() {
        assertEquals("", Joiner.on("").join(new Object[] {}));
    }

    @Test
    public void normal() {
        assertEquals("1 2", Joiner.on(' ').join(1, 2));
    }

    @Test
    public void skipNulls() {
        assertEquals("1,3", Joiner.on(',').skipNulls().join(1, null, 3));
    }

    @Test
    public void useForNull() {
        assertEquals("null, null, 3", Joiner.on(", ").useForNull("null").join(null, null, 3));
    }

    @Test
    public void useForNullSkip() {
        assertEquals("null, null, 3", Joiner.on(", ").skipNulls().useForNull("null").join(null, null, 3));
    }

    @Test
    public void trim() {
        assertEquals("a\nb\nc\n", Joiner.on('\n').trim().join(" a", "b", "c ", " "));
    }

    @Test
    public void skipEmpty() {
        assertEquals("1,3", Joiner.on(',').skipEmpty().trim().useForNull("").join(1, "", null, " ", 3));
    }
}
