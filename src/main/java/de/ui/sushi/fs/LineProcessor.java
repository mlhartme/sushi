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

/** Reads a node line-by-line. In some sense, this class is similar to Buffer, but operates on chars. */
public abstract class LineProcessor {
    public static final int INITIAL_BUFFER_SIZE = 2;
    
    private char[] buffer;
    
    private boolean trim;
    private boolean empty;
    
    private int start;
    private int end;
    
    private Node node;
    private int line;
    private String comment;
    
    public LineProcessor() {
        this(false, true, null);
    }

    public LineProcessor(boolean trim, boolean empty, String comment) {
        this(INITIAL_BUFFER_SIZE, trim, empty, comment);
    }

    public LineProcessor(int bufferSize, boolean trim, boolean empty, String comment) {
        this.buffer = new char[bufferSize];
        this.trim = trim;
        this.empty = empty;
        this.comment = comment;
    }
    
    public int run(Node node) throws IOException {
        return run(node, node.getIO().getSettings().lineSeparator);
    }

    public int run(Node node, String separator) throws IOException {
        Reader src;

        src = node.createReader();
        run(node, 1, src, separator);
        src.close();
        return line;
    }

    public void run(Node node, int startLine, Reader src, String separator) throws IOException {
        int sepLen;
        int len;
        int idx;
        char[] newBuffer;
        
        sepLen = separator.length();
        this.node = node;
        this.line = startLine;
        
        start = 0;
        end = 0;
        while (true) {
            len = src.read(buffer, end, buffer.length - end);
            if (len == -1) {
                if (start != end) {
                    doLine(new String(buffer, start, end - start));
                }
                return;
            } else {
                end += len;
            }
            while (true) {
                idx = next(separator);
                if (idx == -1) {
                    break;
                }
                doLine(new String(buffer, start, idx - start));
                start = idx + sepLen;
            }
            if (end == buffer.length) {
                if (start == 0) {
                    newBuffer = new char[buffer.length * 3 / 2 + 10];
                    System.arraycopy(buffer, 0, newBuffer, 0, end);
                    buffer = newBuffer;
                } else {
                    System.arraycopy(buffer, start, buffer, 0, end - start);
                    end -= start;
                    start = 0;
                }
            }
        }
    }

    private int next(String separator) {
        int j;
        int max;
        
        max = separator.length();
        for (int i = start; i <= end - max; i++) {
            for (j = 0; j < max; j++) {
                if (separator.charAt(j) != buffer[i + j]) {
                    break;
                }
            }
            if (j == max) {
                return i;
            }
        }
        return -1;
    }

    private void doLine(String str) throws IOException {
        if (trim) {
            str = str.trim();
        }
        if (empty || str.length() > 0) {
            if (comment == null || !str.startsWith(comment)) {
                line(str);
            }
        }
        line++;
    }
    
    public abstract void line(String line) throws IOException;
    
    public Node getNode() {
        return node;
    }
    
    public int getLine() {
        return line;
    }
}
