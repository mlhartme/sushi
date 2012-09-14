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

import java.util.regex.Pattern;

public class LineFormat {
    // order is important
    public static final Pattern GENERIC_SEPARATOR = Pattern.compile(Pattern.quote("\n\r") + "|" + Pattern.quote("\r\n")  + "|" + Pattern.quote("\n") + "|" + Pattern.quote("\r"));
    public static final Pattern LF_SEPARATOR = Pattern.compile(Pattern.quote("\n"));

    /** how to trim lines before they are returned by next() */
    public static enum Trim {
        NOTHING, SEPARATOR, ALL
    }


    public static final Pattern NO_EXCLUDES = Pattern.compile("\\za");

    public static final LineFormat RAW_FORMAT = new LineFormat(GENERIC_SEPARATOR, Trim.NOTHING);

    public static Pattern excludes(boolean empty, String ... comments) {
        StringBuilder builder;

        if (!empty && comments.length == 0) {
            return NO_EXCLUDES;
        }
        builder = new StringBuilder();
        if (empty) {
            builder.append(Pattern.quote(""));
        }
        for (String comment : comments) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(Pattern.quote(comment));
            builder.append(".*|");
        }
        return Pattern.compile(builder.toString());
    }

    /** line separator */
    public final Pattern separator;

    /** line trimming mode */
    public final Trim trim;

    /** line that match this pattern after trimming will be excluded, they are never return by next. */
    public final Pattern excludes;

    public LineFormat(Pattern separator) {
        this(separator, Trim.SEPARATOR);
    }

    public LineFormat(Pattern separator, Trim trim) {
        this(separator, trim, NO_EXCLUDES);
    }

    public LineFormat(Pattern separator, Trim trim, Pattern excludes) {
        if (separator.matcher("").matches()) {
            throw new IllegalArgumentException(separator.pattern());
        }
        this.separator = separator;
        this.trim = trim;
        this.excludes = excludes;
    }
}
