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

import org.junit.Test;
import static org.junit.Assert.*;

public class LineProcessorTest {
    IO io = new IO();
    
    @Test
    public void empty() throws IOException {
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
    public void comment() throws IOException {
        LineCollector collector;
        List<String> result;
        
        collector = new LineCollector(100, true, false, "//");
        result = collector.collect(io.stringNode("first\n // \n\n//comment\nlast"));
        assertEquals(Arrays.asList("first", "last"), result);
    }

    //--
    
    private void check(String str, String ... expected) throws IOException {
        check(1024, str, expected);
        check(10, str, expected);
        check(1, str, expected);
    }

    private void check(int initialSize, String str, String ... expected) throws IOException {
        LineCollector collector;
        List<String> result;
        
        collector = new LineCollector(initialSize, false, true, null);
        result = collector.collect(io.stringNode(str));
        assertEquals(Arrays.asList(expected), result);
        assertEquals(expected.length + 1, collector.getLine());
    }
}
