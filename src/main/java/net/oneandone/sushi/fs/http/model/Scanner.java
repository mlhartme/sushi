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
package net.oneandone.sushi.fs.http.model;

public class Scanner {
    private int pos;

    public Scanner() {
        this.pos = 0;
    }

    public String parseProtocol(String str) {
        int start;

        skipWhitespace(str);
        start = pos;
        if (str.startsWith(StatusLine.HTTP_1_1, start)) {
            pos = start + StatusLine.HTTP_1_1.length();
            return StatusLine.HTTP_1_1;
        } else {
            pos = str.indexOf(' ', start);
            if (pos == -1) {
                pos = str.length();
            }
            return str.substring(start, pos);
        }
    }

    public int skipWhitespace(String str) {
        while ((pos < str.length()) && isWhitespace(str.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    //--

    public static boolean isWhitespace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
    }

    public static String substringTrimmed(String str, int start, int end) {
        int theStart;
        int theEnd;

        theStart = start;
        theEnd = end;

        while (theStart < end && isWhitespace(str.charAt(theStart))) {
            theStart++;
        }
        while (theEnd > theStart && isWhitespace(str.charAt(theEnd - 1))) {
            theEnd--;
        }
        return str.substring(theStart, theEnd);
    }
}
