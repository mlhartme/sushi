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
package net.oneandone.sushi.fs.multi;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.util.Util;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
