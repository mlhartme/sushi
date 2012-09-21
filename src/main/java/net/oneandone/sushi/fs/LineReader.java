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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/** Reads a node line-by-line. In some sense, this class is similar to Buffer, but operates on chars. */
public class LineReader implements AutoCloseable {
    public static final int INITIAL_BUFFER_SIZE = 256;

    private final Reader reader;

    /** line separator */
    private final LineFormat format;

    /** current line number */
    private int line;

    private final CharArraySequence buffer;

    public LineReader(Reader reader, LineFormat format) {
        this(reader, format, INITIAL_BUFFER_SIZE);
    }

    public LineReader(Reader reader, LineFormat format, int initialBufferSize) {
        this.reader = reader;
        this.format = format;
        this.line = 0;
        this.buffer = new CharArraySequence(0, 0, new char[initialBufferSize]);
    }

    //--

    /** @return number of the line return by the last call to next. First line has number 1. */
    public int getLine() {
        return line;
    }

    public Reader getReader() {
        return reader;
    }

    public LineFormat getFormat() {
        return format;
    }

    public void close() throws IOException {
        reader.close();
    }

    //--

    /** Never closes the underlying reader. @return next line of null for end of file */
    public String next() throws IOException {
        String result;
        int len;
        Matcher matcher;

        while (true) {
            matcher = format.separator.matcher(buffer);
            if (matcher.find()) {
                len = matcher.end();
                if (buffer.start + len == buffer.end) {
                    // make sure we match the longest separator possible
                    if (buffer.isFull()) {
                        buffer.grow();
                    }
                    buffer.fill(reader);
                    matcher = format.separator.matcher(buffer);
                    if (!matcher.find()) {
                        throw new IllegalStateException();
                    }
                    len = matcher.end();
                }
                result = new String(buffer.chars, buffer.start, format.trim == LineFormat.Trim.NOTHING ? len : matcher.start());
                buffer.start += len;
            } else {
                if (buffer.isFull()) {
                    buffer.grow();
                }
                if (buffer.fill(reader)) {
                    continue;
                } else {
                    if (buffer.isEmpty()) {
                        // EOF
                        return null;
                    } else {
                        result = buffer.eat();
                    }
                }
            }

            // always bump, even if we don't return the line 
            line++;
            if (format.trim == LineFormat.Trim.ALL) {
                result = result.trim();
            }
            if (!format.excludes.matcher(result).matches()) {
                return result;
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

    private static class CharArraySequence implements CharSequence {
        private int start;
        private int end;
        private char[] chars;

        public CharArraySequence(int start, int end, char[] chars) {
            this.start = start;
            this.end = end;
            this.chars = chars;
        }

        @Override
        public int length() {
            return end - start;
        }

        @Override
        public char charAt(int index) {
            return chars[start + index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new CharArraySequence(this.start + start, this.start + end, this.chars);
        }

        public boolean isEmpty() {
            return start == end;
        }

        public boolean isFull() {
            return end == chars.length;
        }

        public String eat() {
            String result;

            result = new String(chars, start, end - start);
            end = start;
            return result;
        }

        public void grow() {
            char[] tmp;

            if (start != 0) {
                System.arraycopy(chars, start, chars, 0, end - start);
                end -= start;
                start = 0;
            } else {
                tmp = new char[chars.length * 3 / 2 + 10];
                System.arraycopy(chars, 0, tmp, 0, end);
                chars = tmp;
            }
        }

        public boolean fill(Reader src) throws IOException {
            int len;

            len = src.read(chars, end, chars.length - end);
            if (len != -1) {
                end += len;
                return true;
            } else {
                return false;
            }
        }
    }
}
