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
package net.oneandone.sushi.csv;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CsvTest {
    private static final Format FMT = new Format();
    private static final World WORLD = new World();

    @Test
    public void empty() throws IOException {
        check("");
    }

    @Test
    public void oneWithoutNewline() throws IOException {
        check("a", "a");
    }

    @Test
    public void oneWithNewline() throws IOException {
        check("Abc\n", "Abc");
    }

    @Test
    public void two() throws IOException {
        check("a\nbc\r\n\rd\n",
              "a", "bc", "", "d");
    }

    @Test
    public void twoError() throws IOException {
        String msg;
        int idx;

        try {
            check("\"\n\"");
            fail();
        } catch (CsvExceptions e) {
            msg = e.getMessage();
            idx = msg.indexOf("quote not closed");
            assertTrue(idx != -1);
            idx = msg.indexOf("quote not closed", idx + 1);
            assertTrue(idx != -1);
        }
    }

    //--

    private Csv read(String str) throws IOException {
        return Csv.read(FMT, node(str));
    }

    private Node node(String str) {
        return WORLD.memoryNode(str);
    }

    private void check(String orig, String ... lines) throws IOException {
        Csv csv = read(orig);
        lines(csv, lines);
    }

    private void lines(Csv csv, String ... lines) {
        assertEquals(lines.length, csv.size());
        for (int i = 0; i < lines.length; i++) {
            assertEquals(1, csv.get(i).size());
            assertEquals(Arrays.asList(lines[i]), csv.get(i).get(0));
        }
    }
}
