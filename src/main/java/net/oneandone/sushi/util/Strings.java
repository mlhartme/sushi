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
import java.util.Arrays;
import java.util.Collection;

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

    //--
    
    public static String removeLeft(String str, String left) {
        String result;

        result = removeLeftOpt(str, left);
        if (result != str || left.isEmpty()) {
            return result;
        } else {
            throw new IllegalArgumentException("'" + str + "' does not start with '" + left + "'");
        }
    }

    public static String removeLeftOpt(String str, String left) {
        if (str.startsWith(left)) {
            return str.substring(left.length());
        } else {
            return str;
        }
    }

    public static String removeRight(String str, String right) {
        String result;

        result = removeRightOpt(str, right);
        if (result != str || right.isEmpty()) {
            return result;
        } else {
            throw new IllegalArgumentException("'" + str + "' does not end with '" + right + "'");
        }
    }

    public static String removeRightOpt(String str, String right) {
        if (str.endsWith(right)) {
            return str.substring(0, str.length() - right.length());
        } else {
            return str;
        }
    }

    //-- padding

    public static String padLeft(String str, int count) {
        return padLeft(str, count, ' ');
    }

    public static String padLeft(String str, int count, char ch) {
        for (count -= str.length(); count > 0; count--) {
            str = ch + str;
        }
        return str;
    }

    public static String padRight(String str, int count) {
        return padRight(str, count, ' ');
    }

    public static String padRight(String str, int count, char ch) {
        for (count -= str.length(); count > 0; count--) {
            str = str + ch;
        }
        return str;
    }

    //--

    public static String indent(String str, String space) {
        StringBuilder builder;

        builder = new StringBuilder();
        for (String line : Separator.RAW_LINE.split(str)) {
            builder.append(space);
            builder.append(line);
        }
        return builder.toString();
    }

    //--

    public static String times(char ch, int count) {
        StringBuilder buffer;

        buffer = new StringBuilder();
        while (count-- > 0) {
            buffer.append(ch);
        }
        return buffer.toString();
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
                        result.append("\\u").append(Strings.padLeft(Integer.toHexString(c), '0'));
                    } else {
                        result.append(c);
                    }
            }
        }
        return result.toString();
    }
}

