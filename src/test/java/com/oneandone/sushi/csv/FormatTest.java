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

package com.oneandone.sushi.csv;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FormatTest {
    private final Format format = new Format(false, '/', '\'');

    @Test
    public void empty() throws CsvLineException {
        values("", "\n", "");
    }

    @Test
    public void cellEmpty() throws CsvLineException {
        cell("EMPTY", "EMPTY\n");
        cell("'EMPTY'", "EMPTY\n");
        cell("EMPTY ", "'EMPTY '\n", "EMPTY ");
        cell("'EMPTY '", "'EMPTY '\n", "EMPTY ");
    }

    @Test
    public void cellNull() throws CsvLineException {
        cell("NULL", "NULL\n", (String[]) null);
        cell("NULL ", "'NULL '\n", "NULL ");
    }

    @Test
    public void doubleQuotes() throws CsvLineException {
        cell("''", "\n", "");
        cell("''''", "''''\n", "'");
        cell("'abc''de'", "'abc''de'\n", "abc'de");
        cell("12''345", "'12''345'\n", "12'345");
    }

    @Test
    public void escape() throws CsvLineException {
        cell("/1", "1\n", "1");
        cell("//", "'//'\n", "/");
        cell("/a", "'a'\n", "a");
        cell("/'", "''''\n", "'");
        cell("/|", "'/|'\n", "|");
        cell("/;", "'/;'\n", ";");
    }

    @Test
    public void values() throws CsvLineException {
        values("1", "1\n", "1");
        values("ab", "'ab'\n", "ab");
        values("1;2", "1;2\n", "1", "2");
        values("a;b;c", "'a';'b';'c'\n", "a", "b", "c");
        values(";", ";\n", "", "");
        values("/EMPTY", "'/EMPTY'\n", "EMPTY");
        values("/NULL", "'/NULL'\n", "NULL");
    }

    @Test(expected=CsvLineException.class)
    public void quoteNotClosed() throws CsvLineException {
        values("'", "\n", "");
    }
    
    @Test(expected=CsvLineException.class)
    public void innerQuote() throws CsvLineException {
        values("a'b", "\n", "");
    }

    @Test
    public void valueWhitespace() throws CsvLineException {
        values("1 ", "'1 '\n", "1 ");
        values(" a\t", "' a\t'\n", " a\t");
        values("a\t; b;c", "'a\t';' b';'c'\n", "a\t", " b", "c");
    }

    @Test
    public void cell() throws CsvLineException {
        cell("1", "1\n", "1");
        cell("'1'", "1\n", "1");
        cell("1|2", "'1|2'\n", "1", "2");
        cell("a|b|c", "'a|b|c'\n", "a", "b", "c");
    }

    // whitespace within cells is not removed!
    @Test
    public void cellWhitespace() throws CsvLineException {
        cell("' 1 '", "' 1 '\n", " 1 ");
        cell("1 | 2", "'1 | 2'\n", "1 ", " 2");
        cell("a\t|  b|\t \tc", "'a\t|  b|\t \tc'\n", "a\t", "  b", "\t \tc");
    }

    // test lines where each cell has exacly one value
    private void values(String line, String lineExpected, String ... cellsExpected) throws CsvLineException {
        Line parsed;
        List<String> cell;
        
        parsed = format.read(line);
        assertEquals(cellsExpected.length, parsed.size());
        for (int i = 0; i < cellsExpected.length; i++) {
            cell = parsed.get(i);
            assertEquals(1, cell.size());
            assertEquals(cellsExpected[i], cell.get(0));
        }
        assertEquals(lineExpected, format.write(parsed));
    }

    // test lines with exactly one cell
    private void cell(String line, String lineExpected, String ... valuesExpected) throws CsvLineException {
        Line parsed;
        List<String> lst;
        
        parsed = format.read(line);
        assertEquals(1, parsed.size());
        lst = valuesExpected == null ? null : Arrays.asList(valuesExpected);
        assertEquals(lst, parsed.get(0));
        assertEquals(lineExpected, format.write(parsed));
    }
}
