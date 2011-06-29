package net.sf.beezle.sushi.util;

import java.io.IOException;

/**
 * Joins objects on a separator. Similar to Google's Joiner
 * (http://guava-libraries.googlecode.com/svn/tags/release09/javadoc/com/google/common/base/Joiner.html), but it's not
 * immutable, configuration uses side-effects.
 */
public class Joiner {
    public static final Joiner SPACE = Joiner.on(' ');

    public static Joiner on(char c) {
        return on(Character.toString(c));
    }

    public static Joiner on(String separator) {
        return new Joiner(separator, false, false, null, false);
    }

    //--

    private final String separator;
    private boolean trim;
    private boolean skipNulls;
    private Object useForNull;
    private boolean skipEmpty;

    public Joiner(String separator, boolean trim, boolean skipNulls, Object useForNull, boolean skipEmpty) {
        this.separator = separator;
        this.trim = trim;
        this.skipNulls = skipNulls;
        this.useForNull = useForNull;
        this.skipEmpty = skipEmpty;
    }

    public Joiner trim() {
        trim = true;
        return this;
    }

    public Joiner skipNulls() {
        skipNulls = true;
        return this;
    }

    /** has precedence over skipNulls */
    public Joiner useForNull(Object forNull) {
        this.useForNull = forNull;
        return this;
    }

    public Joiner skipEmpty() {
        skipEmpty = true;
        return this;
    }

    public String join(Object[] array) {
        return join(java.util.Arrays.asList(array));
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

    public String join(Iterable<?> lst) {
        StringBuilder result;

        result = new StringBuilder();
        appendTo(result, lst);
        return result.toString();
    }

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
