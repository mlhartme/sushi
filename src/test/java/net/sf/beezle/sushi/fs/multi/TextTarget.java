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

import static org.junit.Assert.*;

public class TextTarget {
    public static TextTarget create(Node file, int bytes) throws IOException {
        file.writeBytes(text(bytes));
        return new TextTarget(file);
    }

    private static byte[] text(int count) {
        byte[] result;
        byte b;

        result = new byte[count];
        b = 'a';
        for (int i = 0; i < count; i++) {
            result[i] = i % 60 == 0 ? 10 : b;
            b++;
            if (b > 'z') {
                b = 'a';
            }
        }
        return result;
    }

    private final Node file;
    private final byte[] bytes;
    private final String string;
    private final List<String> lines;
    private final long length;
    private final long lastModified;
    private final String md5;

    public TextTarget(Node file) throws IOException {
        this.file = file;
        this.bytes = file.readBytes();
        this.string = file.readString();
        this.lines = file.readLines();
        this.length = file.length();
        this.lastModified = file.getLastModified();
        this.md5 = file.md5();
    }

    public void readBytes() throws IOException {
        byte[] result;

        result = file.readBytes();
        if (!Arrays.equals(bytes, result)) {
            assertEquals(Util.toString(bytes), Util.toString(result));
        }
    }

    public void readString() throws IOException {
        assertEquals(string, file.readString());
    }

    public void readLines() throws IOException {
        assertEquals(lines, file.readLines());
    }

    public void md5() throws IOException {
        assertEquals(md5, file.md5());
    }

    public void length() throws IOException {
        assertEquals(length, file.length());
    }

    public void lastModified() throws IOException {
        assertEquals(lastModified, file.getLastModified());
    }

    public void exists() throws IOException {
        assertTrue(file.exists());
    }

    public void isFile() throws IOException {
        assertTrue(file.isFile());
    }

    public void isDirectory() throws IOException {
        assertFalse(file.isDirectory());
    }
}
