/**
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
package net.sf.beezle.sushi.csv;

import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.World;
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
