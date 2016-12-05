/*
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
package net.oneandone.sushi.io;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BufferTest {
    @Test
    public void readBytes() throws IOException {
        checkReadBytes("0");
        checkReadBytes("01");
        checkReadBytes("012");
        checkReadBytes("0123");
        checkReadBytes("01234");
        checkReadBytes("012345");
    }

    private void checkReadBytes(String data) throws IOException {
        Buffer buffer;
        ByteArrayInputStream src;
        byte[] orig;
        byte[] copy;

        buffer = new Buffer(3);
        orig = data.getBytes();
        if (data.length() != orig.length) {
            throw new IllegalStateException();
        }
        src = new ByteArrayInputStream(orig);
        copy = buffer.readBytes(src);
        assertTrue(Arrays.equals(orig, copy));
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
        long length;
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
