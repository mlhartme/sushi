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

package de.ui.sushi.fs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;
import static org.junit.Assert.*;

public class LineReaderTest {
    Pattern separator;
    IO io = new IO();
    
    @Test
    public void zero() throws IOException {
        check("");
    }

    @Test
    public void one() throws IOException {
        check("abc\n", "abc");
    }

    @Test
    public void oneWithoutNewline() throws IOException {
        check("ab", "ab");
    }

    @Test
    public void two() throws IOException {
        check("abc\n\n123\n", "abc", "", "123");
    }

    @Test
    public void longline() throws IOException {
        String ll = "1234567890abcdefghijklmnopqrstuvwxyz";
        
        ll = ll + ll + ll;
        check(ll + "\n" + ll, ll, ll);
    }

    @Test
    public void comment() {
        check("first\n // \n\n//comment\nlast", LineReader.Trim.ALL, false, "//", 5,
              "first", "last");
    }

    @Test
    public void empty() {
        check("first\n\nthird\n  \nfifth", LineReader.Trim.ALL, false, null, 5,
              "first", "third", "fifth");
    }

    @Test
    public void trimNothing() {
        check("hello\nworld", LineReader.Trim.NOTHING, false, null, 2,
              "hello\n", "world");
    }

    @Test
    public void separators() {
        separator = LineReader.ANY_NEWLINE;
        check("a\nb\rc\r\nd\n\re", LineReader.Trim.NOTHING, false, null, 5,
                "a\n", "b\r", "c\r\n", "d\n\r", "e");
    }

    //--
    
    private void check(String str, String ... expected) {
        check(str, LineReader.Trim.SEPARATOR, true, null, expected.length, expected);
    }

    private void check(String str, LineReader.Trim trim, boolean empty, String comment, int lastLine, String ... expected) {
        check(str, trim, empty, comment, lastLine, 1024, expected);
        check(str, trim, empty, comment, lastLine, 10, expected);
        check(str, trim, empty, comment, lastLine, 7, expected);
        check(str, trim, empty, comment, lastLine, 3, expected);
        check(str, trim, empty, comment, lastLine, 1, expected);
    }

    private void check(String str, LineReader.Trim trim, boolean empty, String comment, int lastLine, int initialSize, String ... expected) {
        Node node;
        LineReader reader;
        List<String> result;

        try {
            node = io.stringNode(str);
            reader = LineReader.create(node, separator == null ? LineReader.separator(node) : separator, trim, empty, comment, initialSize);
            result = reader.collect();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        assertEquals(Arrays.asList(expected), result);
        assertEquals(lastLine, reader.getLine());
    }
}
