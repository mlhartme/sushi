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
package net.oneandone.sushi.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Output in unified format. See http://en.wikipedia.org/wiki/Diff
 */
public class Diff {
    public static String diff(String leftStr, String rightStr) {
        return diff(leftStr, rightStr, false, 0);
    }

    public static String diff(String leftStr, String rightStr, boolean range, int context) {
        return diff(leftStr, rightStr, range, context, false);
    }

    public static String diff(String leftStr, String rightStr, boolean range, int context, boolean escape) {
        return diff(Separator.RAW_LINE.split(leftStr), Separator.RAW_LINE.split(rightStr), range, context, escape);
    }

    public static String diff(List<String> left, List<String> right, boolean range, int context, boolean escape) {
        List<String> commons;
        List<Chunk> chunks;
        Chunk chunk;
        StringBuilder result;
        int ci;
        Chunk last;

        commons = Lcs.compute(left, right);
        chunks = diff(left, commons, right);
        result = new StringBuilder();
        last = null;
        for (int c = 0; c < chunks.size(); c++) {
            chunk = chunks.get(c);
            if (range && (last == null || !last.touches(chunk, context))) {
                addRange(result, chunks, c, commons.size(), context);
            }
            ci = Math.max(chunk.common - context, c == 0 ? 0 : chunks.get(c - 1).common);
            for (int i = ci; i < chunk.common; i++) {
                result.append(" ").append(commons.get(i));
            }
            if (chunk.delete > 0) {
                for (int i = chunk.left; i < chunk.left + chunk.delete; i++) {
                    result.append('-');
                    appendEscaped(left.get(i), escape, result);
                }
            }
            if (chunk.add.size() > 0) {
                for (String line : chunk.add) {
                    result.append("+");
                    appendEscaped(line, escape, result);
                }
            }
            ci = Math.min(chunk.common + context, c == chunks.size() - 1 ? commons.size() : chunks.get(c + 1).common - context);
            for (int i = chunk.common; i < ci; i++) {
                result.append(" ").append(commons.get(i));
            }
            last = chunk;
        }
        return result.toString();
    }

    public static void appendEscaped(String str, boolean escape, StringBuilder dest) {
        int max;
        char c;
        if (escape) {
            max = str.length();
            for (int i = 0; i < max; i++) {
                c = str.charAt(i);
                switch (c) {
                case '\t':
                    dest.append("\\t");
                    break;
                case '\r':
                    dest.append("\\r");
                    break;
                case '\n':
                    dest.append("\\n");
                    break;
                default:
                    if (c < ' ') {
                        dest.append('[').append((int) c).append(']');
                    } else {
                        dest.append(c);
                    }
                }
            }
        } else {
            dest.append(str);
        }
    }
    public static List<Chunk> diff(List<String> left, List<String> commons, List<String> right) {
        Chunk chunk;
        List<Chunk> result;
        int li;
        int ri;
        int lmax;
        int rmax;
        String common;

        result = new ArrayList<>();
        lmax = left.size();
        rmax = right.size();
        li = 0;
        ri = 0;
        for (int ci = 0; ci <= commons.size(); ci++) {
            common = ci < commons.size() ? commons.get(ci) : null;
            if (li < lmax && !left.get(li).equals(common)) {
                chunk = new Chunk(li, ci, ri);
                result.add(chunk);
                do {
                    li++;
                } while (li < lmax && !left.get(li).equals(common));
                chunk.delete = li - chunk.left;
            } else {
                // definite assignment
                chunk = null;
            }
            if (ri < rmax && !right.get(ri).equals(common)) {
                if (chunk == null) {
                    chunk = new Chunk(li, ci, ri);
                    result.add(chunk);
                }
                do {
                    chunk.add.add(right.get(ri++));
                } while (ri < rmax && !right.get(ri).equals(common));
            }
            li++;
            ri++;
        }
        return result;
    }

    private static void addRange(StringBuilder result, List<Chunk> chunks, int ofs, int cmax, int context) {
        Chunk first;
        Chunk prev;
        Chunk current;
        int leftStart;
        int leftCount;
        int rightStart;
        int rightCount;

        first = chunks.get(ofs);
        leftStart = Math.max(first.left - context, 0);
        rightStart = Math.max(first.right - context, 0);
        leftCount = first.left - leftStart;
        rightCount = first.right - rightStart;
        prev = null;
        for ( ; ofs < chunks.size(); ofs++) {
            current = chunks.get(ofs);
            if (prev != null && !prev.touches(current, context)) {
                break;
            }
            leftCount += current.delete;
            rightCount += current.add.size();
            prev = current;
        }
        current = chunks.get(ofs - 1);
        leftCount += current.common - first.common;
        leftCount += Math.min(current.common + context, cmax) - current.common;
        rightCount += current.common - first.common;
        rightCount += Math.min(current.common + context, cmax) - current.common;
        result.append("@@ -");
        addRange(result, leftStart, leftCount);
        result.append(" +");
        addRange(result, rightStart, rightCount);
        result.append(" @@\n");
    }

    private static void addRange(StringBuilder result, int idx, int count) {
        result.append(idx + 1);
        if (count != 1) {
            result.append(',').append(count);
        }
    }

    private static class Chunk {
        public final int left;
        public final int common;
        public final int right;
        public int delete;
        public final List<String> add;

        public Chunk(int left, int common, int right) {
            this.left = left;
            this.right = right;
            this.common = common;
            this.add = new ArrayList<String>();
        }

        public boolean touches(Chunk chunk, int context) {
            return Math.abs(common - chunk.common) <= context * 2;
        }

        public String range() {
            return "@@ -" + (left + 1) + "," + delete + " +" + (right + 1) + "," + add.size() + " @@\n";
        }
    }
}
