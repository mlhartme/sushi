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
import net.sf.beezle.sushi.util.Util;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class Target {
    private final Node small;
    private final byte[] mediumBytes;
    private final Node medium;

    public Target(Node work) throws IOException {
        this.small = work.join("small").writeString("abc");
        this.mediumBytes = bytes(16799);
        this.medium = work.join("medium").writeBytes(mediumBytes);
    }
/*
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
*/
    public void mediumReadBytes() throws IOException {
        byte[] result;

        result = medium.readBytes();
        if (!Arrays.equals(mediumBytes, result)) {
            assertEquals(Util.toString(mediumBytes), Util.toString(result));
        }
    }    

    //--

    private static byte[] bytes(int count) {
        byte[] result;

        result = new byte[count];
        for (int i = 0; i < count; i++) {
            result[i] = (byte) i;
        }
        return result;
    }
    

}
