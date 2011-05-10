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

package net.sf.beezle.sushi.fs.multi;

import net.sf.beezle.sushi.fs.Node;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Target {
    private final Node small;

    public Target(Node work) throws IOException {
        this.small = work.join("foo").writeString("abc");
    }

    public void smallReadBytes() throws IOException {
        assertEquals("abc", small.readString());
    }

    public void smallReadString() throws IOException {
        assertEquals("abc", small.readString());
    }

    public void smallReadLines() throws IOException {
        assertEquals(Arrays.asList("abc"), small.readLines());
    }

    public void smallLength() throws IOException {
        assertEquals(3, small.length());
    }
}
