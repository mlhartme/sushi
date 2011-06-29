package net.sf.beezle.sushi.util;

import java.io.IOException;

/**
 * Joins objects on a separator. A reverse Splitter. Similar to Google's Joiner
 * (http://guava-libraries.googlecode.com/svn/tags/release09/javadoc/com/google/common/base/Joiner.html).
 * Immutable, configuration methods return new instances.
 */
public class Joiner {
    public static final Joiner SPACE = Joiner.on(' ');
    public static final Joiner COMMA = Joiner.on(',');
    public static final Joiner LIST = Joiner.on(", ").trim().useForNull("null");
    public static final Joiner SLASH = Joiner.on('/');

    public static Joiner on(char c) {
        return on(Character.toString(c));
    }

    public static Joiner on(String separator) {
        return new Joiner(separator, false, false, null, false);
    }

    //--

    private final String separator;
    private final boolean trim;
    private final boolean skipNulls;
    private final Object useForNull;
    private final boolean skipEmpty;

    public Joiner(String separator, boolean trim, boolean skipNulls, Object useForNull, boolean skipEmpty) {
        this.separator = separator;
        this.trim = trim;
        this.skipNulls = skipNulls;
        this.useForNull = useForNull;
        this.skipEmpty = skipEmpty;
    }

    public Joiner(Joiner orig) {
        this(orig.separator, orig.trim, orig.skipNulls, orig.useForNull, orig.skipEmpty);
    }

    //-- configuration

    public Joiner trim() {
        return new Joiner(separator, true, skipNulls, useForNull, skipEmpty);
    }

    public Joiner skipNulls() {
        return new Joiner(separator, trim, true, useForNull, skipEmpty);
    }

    /** has precedence over skipNulls */
    public Joiner useForNull(Object forNull) {
        return new Joiner(separator, trim, skipNulls, forNull, skipEmpty);
    }

    public Joiner skipEmpty() {
        return new Joiner(separator, trim, skipNulls, useForNull, true);
    }

    //-- joining

    public String join(Object[] array) {
        return join(java.util.Arrays.asList(array));
    }

    public String join(Iterable<?> lst) {
        StringBuilder result;

        result = new StringBuilder();
        appendTo(result, lst);
        return result.toString();
    }

    public String join(Object first, Object second, Object ... rest) {
        int count;
        StringBuilder result;

        result = new StringBuilder();
        try {
            count = appendTo(result, 0, java.util.Arrays.asList(first));
            count = appendTo(result, count, java.util.Arrays.asList(second));
            appendTo(result, count, java.util.Arrays.asList(rest));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return result.toString();
    }

    //--

    public void appendTo(StringBuilder dest, Iterable<?> lst) {
        try {
            appendTo(dest, 0, lst);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @return number of objects appended */
    public int appendTo(Appendable dest, int count, Iterable<?> lst) throws IOException {
        String str;

        for (Object obj : lst) {
            if (obj == null) {
                if (useForNull != null) {
                    obj = useForNull;
                } else if (skipNulls) {
                    continue;
                } else {
                    throw new NullPointerException();
                }
            }
            str = obj.toString();
            if (trim) {
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
}
