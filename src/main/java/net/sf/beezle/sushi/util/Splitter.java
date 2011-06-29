package net.sf.beezle.sushi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits strings on a separator. A reverse Joiner. Similar to Google's Splitter
 * (http://guava-libraries.googlecode.com/svn/tags/release09/javadoc/com/google/common/base/Splitter.html).
 * Immutable, configuration methods return new instances.
 */
public class Splitter {
    /** skip empty because I want to ignore heading/tailing whitespace */
    public static final Splitter WHITESPACE = Splitter.onPattern("\\s+").skipEmpty();

    public static final Splitter SLASH = Splitter.on('/');
    public static final Splitter LIST = Splitter.on(',').trim();

    public static Splitter on(char c) {
        return on(Character.toString(c));
    }

    public static Splitter on(String separator) {
        return new Splitter(Pattern.compile(separator, Pattern.MULTILINE | Pattern.LITERAL), false, false);
    }

    public static Splitter onPattern(String separator) {
        return on(Pattern.compile(separator, Pattern.MULTILINE));
    }

    public static Splitter on(Pattern pattern) {
        return new Splitter(pattern, false, false);
    }

    //--

    private final Pattern separator;
    private final boolean trim;
    private final boolean skipEmpty;

    public Splitter(Pattern separator, boolean trim, boolean skipEmpty) {
        if (separator.matcher("").find()) {
            throw new IllegalArgumentException(separator.pattern() + " matches the empty string");
        }
        this.separator = separator;
        this.trim = trim;
        this.skipEmpty = skipEmpty;
    }

    public Splitter(Splitter orig) {
        this(orig.separator, orig.trim, orig.skipEmpty);
    }

    //-- configuration

    public Splitter trim() {
        return new Splitter(separator, true, skipEmpty);
    }

    public Splitter skipEmpty() {
        return new Splitter(separator, trim, true);
    }

    //--

    public List<String> split(String str) {
        List<String> lst;

        lst = new ArrayList<String>();
        appendTo(lst, str);
        return lst;
    }

    public void appendTo(List<String> result, String str) {
        int length;
        Matcher matcher;
        int prev;

        length = str.length();
        if (length == 0) {
            return;
        }
        matcher = separator.matcher(str);
        prev = 0;
        while (matcher.find()) {
            add(result, str.substring(prev, matcher.start()));
            prev = matcher.end();
        }
        if (prev <= length) {
            add(result, str.substring(prev, length));
        }
    }

    private void add(List<String> result, String str) {
        if (trim) {
            str = str.trim();
        }
        if (skipEmpty && str.isEmpty()) {
            return;
        }
        result.add(str);
    }
}
