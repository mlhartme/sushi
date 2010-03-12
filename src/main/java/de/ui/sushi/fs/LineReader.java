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
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Reads a node line-by-line. In some sense, this class is similar to Buffer, but operates on chars. */
public class LineReader {
    public static LineReader create(Node node) throws IOException {
        return create(node, Trim.SEPARATOR, true, null);
    }
    
    public static LineReader create(Node node, Trim trim, boolean empty, String comment) throws IOException {
        return create(node, trim, empty, comment, INITIAL_BUFFER_SIZE);
    }

    public static LineReader create(Node node, Trim trim, boolean empty, String comment, int initialBufferSize) throws IOException {
        return new LineReader(node.createReader(), node.getIO().getSettings().lineSeparator, trim, empty, comment, new char[initialBufferSize], 0);
    }

    public static enum Trim {
        NOTHING, SEPARATOR, ALL
    }

    public static final int INITIAL_BUFFER_SIZE = 2;

    private final Reader reader;

    /** line separator */
    private final String separator;
    /** line trimming mode */
    private final Trim trim;
    /** when true, next() returns empty line; otherwise, they're skipped */
    private final boolean empty;
    /** line comment prefix to be skipped; null to disable */
    private final String comment;

    private final int sepLen;
    private final int sepLenTrimmed;

    private int line;

    private char[] buffer;
    private int start;
    private int end;

    public LineReader(Reader reader, String separator, Trim trim, boolean empty, String comment, char[] initialBuffer, int initialLine) {
        this.reader = reader;

        this.separator = separator;
        this.trim = trim;
        this.empty = empty;
        this.comment = comment;

        this.sepLen = separator.length();
        this.sepLenTrimmed = trim == Trim.NOTHING ? sepLen : 0;

        this.line = initialLine;
        this.buffer = initialBuffer;
        this.start = 0;
        this.end = 0;
    }

    //--

    public char[] getBuffer() {
        return buffer;
    }

    /** @return number of the line return by the last call to next */
    public int getLine() {
        return line;
    }

    public Reader getReader() {
        return reader;
    }

    public void close() throws IOException {
        reader.close();
    }

    //--

    /** Never closes the underlying reader. @return next line of null for end of file */
    public String next() throws IOException {
        int idx;
        char[] newBuffer;
        String result;
        int len;

        while (true) {
            idx = findSeparator();
            if (idx != -1) {
                result = new String(buffer, start, idx - start + sepLenTrimmed);
                start = idx + sepLen;
            } else {
                if (end == buffer.length) {
                    if (start != 0) {
                        System.arraycopy(buffer, start, buffer, 0, end - start);
                        end -= start;
                        start = 0;
                    } else {
                        newBuffer = new char[buffer.length * 3 / 2 + 10];
                        System.arraycopy(buffer, 0, newBuffer, 0, end);
                        buffer = newBuffer;
                    }
                }
                len = reader.read(buffer, end, buffer.length - end);
                if (len == -1) {
                    if (start != end) {
                        result = new String(buffer, start, end - start);
                        start = end;
                    } else {
                        // EOF
                        return null;
                    }
                } else {
                    end += len;
                    continue;
                }
            }

            // always bump, even if we don't return the line 
            line++;
            if (trim == Trim.ALL) {
                result = result.trim();
            }
            if (comment == null || !result.startsWith(comment)) {
                if (empty || !result.isEmpty()) {
                    return result;
                }
            }
        }
    }

    public List<String> collect() throws IOException {
        return collect(new ArrayList<String>());
    }

    /** @return result */
    public List<String> collect(List<String> result) throws IOException {
        String line;

        while (true) {
            line = next();
            if (line == null) {
                close();
                return result;
            }
            result.add(line);
        }
    }

    private int findSeparator() {
        int j;

        for (int idx = start; idx <= end - sepLen; idx++) {
            for (j = 0; j < sepLen; j++) {
                if (separator.charAt(j) != buffer[idx + j]) {
                    break;
                }
            }
            if (j == sepLen) {
                return idx;
            }
        }
        return -1;
    }
}
