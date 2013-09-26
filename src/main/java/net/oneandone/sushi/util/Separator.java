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

import net.oneandone.sushi.fs.LineFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits and joins strings on a separator. Similar to Google's Splitter
 * http://guava-libraries.googlecode.com/svn/tags/release09/javadoc/com/google/common/base/Splitter.html and
 * Joiner http://guava-libraries.googlecode.com/svn/tags/release09/javadoc/com/google/common/base/Joiner.html.
 * Immutable, configuration methods return new instances.
 */
public class Separator {
    /** Whitespace delimited lists. Skip empty because I want to ignore heading/tailing whitespace */
    public static final Separator SPACE = Separator.on(" ", Pattern.compile("\\s+")).skipEmpty();

    /** Separator in user-supplied lists. */
    public static final Separator COMMA = Separator.on(',').trim().skipEmpty().forNull("null");

    /** Skip empty remove the last line */
    public static final Separator RAW_LINE = Separator.on("\n", LineFormat.GENERIC_SEPARATOR).trim(LineFormat.Trim.NOTHING).skipEmpty();

    public static Separator on(char c) {
        return on(Character.toString(c));
    }

    public static Separator on(String separator) {
        return new Separator(separator, Pattern.compile(Pattern.quote(separator)));
    }

    public static Separator on(String separator, Pattern pattern) {
        return new Separator(separator, pattern);
    }

    //--

    private final String separator;
    private final Pattern pattern;
    private final LineFormat.Trim trim;
    private final boolean skipEmpty;
    private final String forNull;
    private final boolean skipNull;

    public Separator(String separator, Pattern pattern) {
        this(separator, pattern, LineFormat.Trim.SEPARATOR, false, null, false);
    }

    public Separator(String separator, Pattern pattern, LineFormat.Trim trim, boolean skipEmpty, String forNull, boolean skipNull) {
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("Empty separator");
        }
        if (pattern.matcher("").find()) {
            throw new IllegalArgumentException(pattern.pattern() + " matches the empty string");
        }
        if (!pattern.matcher(separator).find()) {
            throw new IllegalArgumentException("Separtor " + separator + " does not match pattern " + pattern.pattern());
        }
        this.separator = separator;
        this.pattern = pattern;
        this.trim = trim;
        this.skipEmpty = skipEmpty;
        this.forNull = forNull;
        this.skipNull = skipNull;
    }

    public Separator(Separator orig) {
        this(orig.separator, orig.pattern, orig.trim, orig.skipEmpty, orig.forNull, orig.skipNull);
    }

    //--

    public String getSeparator() {
        return separator;
    }

    //-- configuration

    /** Trim elements before joining or after splitting */
    public Separator trim() {
        return trim(LineFormat.Trim.ALL);
    }

    /** Trim elements before joining or after splitting */
    public Separator trim(LineFormat.Trim trim) {
        return new Separator(separator, pattern, trim, skipEmpty, forNull, skipNull);
    }

    /** Do not join empty elements and to not return them from splitting. */
    public Separator skipEmpty() {
        return new Separator(separator, pattern, trim, true, forNull, skipNull);
    }

    public Separator forNull(String forNull) {
        return new Separator(separator, pattern, trim, skipEmpty, forNull, skipNull);
    }

    public Separator skipNull() {
        return new Separator(separator, pattern, trim, skipEmpty, forNull, true);
    }

    //-- joining

    public String join(Object[] array) {
        return join(java.util.Arrays.asList(array));
    }

    public String join(Iterable<?> lst) {
        StringBuilder result;

        result = new StringBuilder();
        joinTo(result, lst);
        return result.toString();
    }

    public String join(Object first, Object second, Object ... rest) {
        int count;
        StringBuilder result;

        result = new StringBuilder();
        try {
            count = joinTo(result, 0, java.util.Arrays.asList(first));
            count = joinTo(result, count, java.util.Arrays.asList(second));
            joinTo(result, count, java.util.Arrays.asList(rest));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result.toString();
    }

    //--

    public void joinTo(StringBuilder dest, Iterable<?> lst) {
        try {
            joinTo(dest, 0, lst);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return number of objects appended */
    public int joinTo(Appendable dest, int count, Iterable<?> lst) throws IOException {
        String str;

        for (Object obj : lst) {
            if (obj == null) {
                if (forNull != null) {
                    obj = forNull;
                } else if (skipNull) {
                    continue;
                } else {
                    throw new NullPointerException();
                }
            }
            str = obj.toString();
            if (trim == LineFormat.Trim.ALL) {
                str = str.trim();
            }
            if (skipEmpty && str.isEmpty()) {
                continue;
            }
            if (count > 0) {
                dest.append(separator);
            }
            count++;
            dest.append(str);
        }
        return count;
    }

    //-- split

    public List<String> split(CharSequence str) {
        List<String> lst;

        lst = new ArrayList<>();
        splitTo(lst, str);
        return lst;
    }

    public void splitTo(List<String> dest, CharSequence str) {
        int length;
        Matcher matcher;
        int prev;

        length = str.length();
        if (length == 0) {
            return;
        }
        matcher = pattern.matcher(str);
        prev = 0;
        while (matcher.find()) {
            add(dest, str.subSequence(prev, trim == LineFormat.Trim.NOTHING ? matcher.end() : matcher.start()));
            prev = matcher.end();
        }
        if (prev <= length) {
            add(dest, str.subSequence(prev, length));
        }
    }

    private void add(List<String> result, CharSequence cs) {
        String str;

        str = cs.toString();
        if (trim == LineFormat.Trim.ALL) {
            str = str.trim();
        }
        if (skipEmpty && str.isEmpty()) {
            return;
        }
        result.add(str);
    }
}
