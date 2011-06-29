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

package net.sf.beezle.sushi.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public class Strings {
    //
    //-- one string
    //

    public static String toHex(byte ... bytes) {
        StringBuilder result;

        result = new StringBuilder();
        toHex(result, bytes);
        return result.toString();
    }

    public static void toHex(StringBuilder result, byte ... bytes) {
        for (byte b : bytes) {
            result.append(Integer.toString(b >> 4 & 0xf, 16));
            result.append(Integer.toString(b & 0xf, 16));
        }
    }

    // TODO
    private static final char SEPARATOR = '\n';

    public static List<String> lines(String all) {
        int ofs;
        int prev;
        List<String> result;

        ofs = all.indexOf(SEPARATOR);
        prev = 0;
        result = new ArrayList<String>();
        while (ofs != -1) {
            ofs++;
            result.add(all.substring(prev, ofs));
            prev = ofs;
            ofs = all.indexOf(SEPARATOR, prev);
        }
        if (prev < all.length()) {
            result.add(all.substring(prev));
        }
        return result;
    }

    public static String removeStart(String str, String start) {
        String result;

        result = removeStartOpt(str, start);
        if (result != str) {
            return result;
        } else {
            throw new IllegalArgumentException("'" + str + "' does not start with '" + start + "'");
        }
    }

    public static String removeStartOpt(String str, String start) {
        if (str.startsWith(start)) {
            return str.substring(start.length());
        } else {
            return str;
        }
    }

    public static String removeEnd(String str, String end) {
        String result;

        result = removeEndOpt(str, end);
        if (result != str) {
            return result;
        } else {
            throw new IllegalArgumentException("'" + str + "' does not end with '" + end + "'");
        }
    }

    public static String removeEndOpt(String str, String end) {
        if (str.endsWith(end)) {
            return str.substring(0, str.length() - end.length());
        } else {
            return str;
        }
    }

    public static String stripExtension(String f) {
        int dotP;

        dotP = f.lastIndexOf('.');
        if (dotP <= 0) {
            return f;
        }
        return f.substring(0, dotP);
    }

    public static String getFileExtension(String f) {
        int idx;

        idx = f.lastIndexOf('.');
        if (idx <= 0 || idx == f.length() - 1) {
            return "";
        }
        return f.substring(idx + 1);
    }

    public static String indent(String str, String space) {
        StringBuilder builder;

        builder = new StringBuilder();
        for (String line : lines(str)) {
            builder.append(space);
            builder.append(line);
        }
        return builder.toString();
    }

    public static String lfill(int count, String str) {
        return lfill(' ', count, str);
    }

    public static String lfill(char ch, int count, String str) {
        for (count -= str.length(); count > 0; count--) {
            str = ch + str;
        }
        return str;
    }

    public static String rfill(int count, String str) {
        return rfill(' ', count, str);
    }

    public static String rfill(char ch, int count, String str) {
        for (count -= str.length(); count > 0; count--) {
            str = str + ch;
        }
        return str;
    }

    public static String times(char ch, int count) {
        StringBuilder buffer;

        buffer = new StringBuilder();
        while (count-- > 0) {
            buffer.append(ch);
        }
        return buffer.toString();
    }

    public static List<String> split(String separator, String str) {
        List<String> lst;

        lst = new ArrayList<String>();
        split(separator, str, lst);
        return lst;
    }

    public static void split(String separator, String str, List<String> result) {
        int skip;
        int idx;
        int prev;

        if (separator.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (str.length() > 0) {
            skip = separator.length();
            idx = str.indexOf(separator);
            prev = 0;
            while (idx != -1) {
                result.add(str.substring(prev, idx));
                prev = idx + skip;
                idx = str.indexOf(separator, prev);
            }
            result.add(str.substring(prev));
        }
    }

    public static String replace(String str, String in, String out) {
        StringBuilder buffer;
        int inLen;
        int idx;
        int prev;

        inLen = in.length();
        if (inLen == 0) {
            throw new IllegalArgumentException();
        }
        buffer = new StringBuilder();
        idx = str.indexOf(in);
        prev = 0;
        while (idx != -1) {
            buffer.append(str.substring(prev, idx));
            buffer.append(out);
            prev = idx + inLen;
            idx = str.indexOf(in, prev);
        }
        buffer.append(str.substring(prev));
        return buffer.toString();
    }

    public static String getCommon(String left, String right) {
        int i;
        int max;

        max = Math.min(left.length(), right.length());
        for (i = 0; i < max; i++) {
            if (left.charAt(i) != right.charAt(i)) {
                break;
            }
        }
        return left.substring(0, i);
    }

    public static int count(String str, String part) {
        int count;
        int idx;
        int len;

        len = part.length();
        idx = 0;
        for (count = 0; true; count++) {
            idx = str.indexOf(part, idx);
            if (idx == -1) {
                return count;
            }
            idx += len;
        }
    }

    public static String block(String prefix, String body, int width, String suffix) {
        return block(prefix, prefix, body, width, suffix, suffix);
    }

    public static String block(String first, String prefix, String body, int width, String suffix, String last) {
        String currentPrefix;
        StringBuilder buffer;
        int space;
        int word;
        int line;
        boolean empty;  // false if at least one word was added to the line

        buffer = new StringBuilder();
        word = skip(body, 0, true);
        currentPrefix = first;
        while (true) {
            buffer.append(currentPrefix);
            line = 0;
            empty = true;
            while (true) {
                space = skip(body, word, false);
                if (space == word) {
                    buffer.append(last);
                    return buffer.toString();
                }
                line += space - word;
                if (empty) {
                    empty = false;
                } else {
                    line++;
                    if (line > width) {
                        break;
                    }
                    buffer.append(' ');
                }
                buffer.append(body.substring(word, space));
                word = skip(body, space, true);
            }
            buffer.append(suffix);
            currentPrefix = prefix;
        }
    }

    public static int skip(String str, int start, boolean ws) {
        int i;
        int max;

        max = str.length();
        for (i = start; i < max; i++) {
            if (Character.isWhitespace(str.charAt(i)) != ws) {
                break;
            }
        }
        return i;
    }

    public static String capitalize(String str) {
        if (str.length() == 0) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static String decapitalize(String str) {
        if (str.length() == 0) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    //-- TODO: dump?
    public static String[] separate(String str, char delim) {
        int i;
        StringTokenizer tokenizer;
        int max;
        String[] result;

        tokenizer = new StringTokenizer(str, "" + delim);
        max = tokenizer.countTokens();
        result = new String[max];
        for (i = 0; i < max; i++) {
            result[i] = tokenizer.nextToken();
        }
        return result;
    }

    // TODO: dump?
    public static String next(String all, int[] idxResult, String ... delimiters) {
        int start;
        int tmp;
        int end;
        int next;

        start = idxResult[0];
        end = all.length();
        next = end; // dummy
        for (String delim : delimiters) {
            tmp = all.indexOf(delim, start);
            if (tmp != -1 && tmp < end) {
                end = tmp;
                next = tmp + delim.length();
            }
        }
        if (end == all.length()) {
            // not changed
            if (start == all.length()) {
                return null;
            } else {
                idxResult[0] = all.length();
                return all.substring(start);
            }
        } else {
            idxResult[0] = next;
            return all.substring(start, end);
        }
    }


    //-- string collections or arrays

    public static final String[] NONE = new String[] {};

    /**
     * Turns a list of Strings into an array.
     *
     * @param coll   collection of Strings
     *
     * @return never null
     */
    public static String[] toArray(Collection<String> coll) {
        String[] ar;

        ar = new String[coll.size()];
        coll.toArray(ar);
        return ar;
    }

    public static ArrayList<String> toList(String ... elements) {
        return new ArrayList<String>(Arrays.asList(elements));
    }


    public static String[] cons(String car, String[] cdr) {
        String[] result;

        result = new String[1 + cdr.length];
        result[0] = car;
        System.arraycopy(cdr, 0, result, 1, cdr.length);
        return result;
    }

    public static String[] cdr(String[] args) {
        String[] result;

        if (args.length == 0) {
            throw new RuntimeException();
        }
        result = new String[args.length - 1];
        System.arraycopy(args, 1, result, 0, result.length);
        return result;
    }

    public static String[] append(String[] ...args) {
        String[] result;
        int length;
        int ofs;

        length = 0;
        for (String[] current : args) {
            length += current.length;
        }
        result = new String[length];
        ofs = 0;
        for (String[] current : args) {
            System.arraycopy(current, 0, result, ofs, current.length);
            ofs += current.length;
        }
        return result;
    }

    /** escape Strings as in Java String literals */
    public static String escape(String str) {
        int i, max;
        StringBuilder result;
        char c;

        max = str.length();
        for (i = 0; i < max; i++) {
            if (str.charAt(i) < 32) {
                break;
            }
        }
        if (i == max) {
            return str;
        }
        result = new StringBuilder(max + 10);
        for (i = 0; i < max; i++) {
            c = str.charAt(i);
            switch (c) {
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                default:
                    if (c < 32) {
                        result.append("\\u").append(Strings.lfill('0', Integer.toHexString(c)));
                    } else {
                        result.append(c);
                    }
            }
        }
        return result.toString();
    }

}

