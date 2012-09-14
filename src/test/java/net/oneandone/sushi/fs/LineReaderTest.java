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
package net.oneandone.sushi.fs;

import net.oneandone.sushi.io.OS;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LineReaderTest {
    private final World world = new World();

    @Test
    public void zero() {
        check("");
    }

    @Test
    public void one() {
        check(l("abc"), "abc");
    }

    @Test
    public void oneWithoutNewline() {
        check("ab", "ab");
    }

    @Test
    public void two() {
        check(l("abc", "", "123"), "abc", "", "123");
    }

    @Test
    public void longline() {
        String ll = "1234567890abcdefghijklmnopqrstuvwxyz";
        
        ll = ll + ll + ll;
        check(l(ll, ll), ll, ll);
    }

    @Test
    public void comment() {
        check(l("first", "", " // ", "//comment") + "last",
                new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.ALL, LineFormat.excludes(true, "//")), 5,
                "first", "last");
    }

    @Test
    public void excludeEmpty() {
        check(l("first", "", "third", "  ", "fifth"),
                new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.ALL, LineFormat.excludes(true)), 5,
                "first", "third", "fifth");
    }

    @Test
    public void trimNothing() {
        check("hello\nworld\r\n",
                new LineFormat(LineFormat.LF_SEPARATOR, LineFormat.Trim.NOTHING, LineFormat.NO_EXCLUDES), 2,
                "hello\n", "world\r\n");
    }

    @Test
    public void separators() {
        check("a\nb\rc\r\nd\n\re", LineFormat.RAW_FORMAT,
                5, "a\n", "b\r", "c\r\n", "d\n\r", "e");
    }

    //--
    
    private void check(String str, String ... expected) {
        check(str, world.getSettings().lineFormat, expected.length, expected);
    }

    private void check(String str, LineFormat format, int lastLine, String ... expected) {
        check(str, format, lastLine, 1024, expected);
        check(str, format, lastLine, 10, expected);
        check(str, format, lastLine, 7, expected);
        check(str, format, lastLine, 3, expected);
        check(str, format, lastLine, 1, expected);
    }

    private void check(String str, LineFormat format, int lastLine, int initialSize, String ... expected) {
        Node node;
        LineReader reader;
        List<String> result;

        try {
            node = world.memoryNode(str);
            reader = new LineReader(node.createReader(), format, initialSize);
            result = reader.collect();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        assertEquals(Arrays.asList(expected), result);
        assertEquals(lastLine, reader.getLine());
    }
    
    private static String l(String ... lines) {
    	return OS.CURRENT.lines(lines);
    }
}
