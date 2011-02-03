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
