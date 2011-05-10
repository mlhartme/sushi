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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Target {
    private final Node small;
    private final byte[] mediumBytes;
    private final Node medium;
    private final String mediumString;
    private final long mediumLastModified;
    private final String mediumMd5;
    private final Node dir;

    public Target(Node work) throws IOException {
        this.small = work.join("small").writeString("abc");
        this.mediumBytes = bytes(16799);
        this.medium = work.join("medium").writeBytes(mediumBytes);
        this.mediumString = medium.readString();
        this.mediumLastModified = medium.getLastModified();
        this.mediumMd5 = medium.md5();
        this.dir = work.join("emptydir").mkdir();
        dir.join("one").mkfile();
        dir.join("two").mkfile();

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

    public void mediumReadBytes() throws IOException {
        byte[] result;

        result = medium.readBytes();
        if (!Arrays.equals(mediumBytes, result)) {
            assertEquals(Util.toString(mediumBytes), Util.toString(result));
        }
    }

    public void mediumReadString() throws IOException {
        assertEquals(mediumString, medium.readString());
    }

    public void mediumMd5() throws IOException {
        assertEquals(mediumMd5, medium.md5());
    }

    public void mediumLastModified() throws IOException {
        assertEquals(mediumLastModified, medium.getLastModified());
    }

    public void mediumExists() throws IOException {
        assertTrue(medium.exists());
    }

    public void mediumIsFile() throws IOException {
        assertTrue(medium.isFile());
    }

    public void dirIsDirectory() throws IOException {
        assertTrue(dir.isDirectory());
    }

    public void dirFind() throws IOException {
        List<? extends Node> lst;

        lst = dir.list();
        assertTrue(lst.contains(dir.join("one")));
        assertTrue(lst.contains(dir.join("two")));
        assertEquals(2, lst.size());
    }

    public void dirList() throws IOException {
        List<? extends Node> lst;

        lst = dir.list();
        assertTrue(lst.contains(dir.join("one")));
        assertTrue(lst.contains(dir.join("two")));
        assertEquals(2, lst.size());
    }

    //--

    private static byte[] bytes(int count) {
        byte[] result;
        byte b;

        result = new byte[count];
        b = 32;
        for (int i = 0; i < count; i++) {
            result[i] = b;
            b++;
            if (b > 'z') {
                b = 32;
            }
        }
        return result;
    }
    

}
