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
package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.fs.http.io.AsciiInputStream;
import net.oneandone.sushi.fs.http.io.AsciiOutputStream;
import net.oneandone.sushi.fs.http.io.ChunkedInputStream;
import net.oneandone.sushi.fs.http.io.ChunkedOutputStream;
import net.oneandone.sushi.io.Buffer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ChunkTest {
    @Test
    public void normal() throws IOException {
        int c;
        ByteArrayOutputStream data;
        ChunkedOutputStream dest;
        ChunkedInputStream src;

        data = new ByteArrayOutputStream();
        dest = new ChunkedOutputStream(5, new AsciiOutputStream(data, 9));
        dest.write("hello, world\n".getBytes());
        dest.write("second line".getBytes());
        dest.close();
        src = new ChunkedInputStream(new AsciiInputStream(new ByteArrayInputStream(data.toByteArray()), 10), new Buffer());
        data = new ByteArrayOutputStream();
        while (true) {
            c = src.read();
            if (c == -1) {
                break;
            }
            data.write(c);
        }
        assertEquals(-1, src.read());
        assertEquals(-1, src.read());
        assertEquals("hello, world\nsecond line", new String(data.toByteArray()));
    }

    // make sure that underlying stream is not read beyond the EOF marker chunk
    @Test
    public void eof() throws IOException {
        ByteArrayOutputStream data;
        ChunkedOutputStream dest;
        AsciiInputStream srcBytes;
        ChunkedInputStream src;
        byte[] moreBytes;

        data = new ByteArrayOutputStream();
        dest = new ChunkedOutputStream(5, new AsciiOutputStream(data, 9));
        dest.write(new byte[0]);
        dest.close();
        moreBytes = new byte[data.size() + 2];
        moreBytes[moreBytes.length - 2] = 9;
        moreBytes[moreBytes.length - 1] = 10;
        System.arraycopy(data.toByteArray(), 0, moreBytes, 0, data.size());
        srcBytes = new AsciiInputStream(new ByteArrayInputStream(moreBytes), 10);
        src = new ChunkedInputStream(srcBytes, new Buffer());
        assertEquals(-1, src.read());
        assertEquals(-1, src.read());
        assertEquals(2, srcBytes.available());
        assertEquals(9, srcBytes.read());
        assertEquals(10, srcBytes.read());
        assertEquals(-1, srcBytes.read());
    }
}
