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

package net.sf.beezle.sushi.io;

import net.sf.beezle.sushi.fs.Settings;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class BufferTest {
    @Test
    public void readLine() throws IOException {
        Buffer buffer;
        Settings settings;
        InputStream src;
        
        buffer = new Buffer();
        settings = new Settings();
        
        src = new ByteArrayInputStream(new byte[] { });
        assertNull(buffer.readLine(src, settings.encoding));
        
        src = new ByteArrayInputStream(new byte[] { 'a', 'b', 'c', '\n', 'a', '\n', '\n', 'x', 'x' });
        assertEquals("abc", buffer.readLine(src, settings.encoding));
        assertEquals("a", buffer.readLine(src, settings.encoding));
        assertEquals("", buffer.readLine(src, settings.encoding));
        try {
            assertEquals("", buffer.readLine(src, settings.encoding));
            fail();
        } catch (EOFException e) {
            // ok
        }
    }

    @Test
    public void copy() throws IOException {
        copy(bytes(), bytes(), 100);
        copy(bytes(0, 1, 2, 3, 4), bytes(0, 1, 2, 3, 4), 100);
        copy(bytes(), bytes(0), 0);
        copy(bytes(0), bytes(0, 1, 2, 3, 4), 1);
    }

    private byte[] bytes(int ... data) {
        byte[] result;
        
        result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) data[i];
        }
        return result;
    }
    
    private void copy(byte[] expected, byte[] data, int max) throws IOException {
        Buffer buffer;
        InputStream src;
        ByteArrayOutputStream dest;
        int length;
        byte[] found;
        
        buffer = new Buffer(1);
        src = new ByteArrayInputStream(data);
        dest = new ByteArrayOutputStream();
        length = buffer.copy(src, dest, max);
        found = dest.toByteArray();
        assertEquals(length, found.length);
        assertEquals(expected.length, found.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], found[i]);
        }
    }
}
