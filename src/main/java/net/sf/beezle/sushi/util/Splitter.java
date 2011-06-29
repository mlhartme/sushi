package net.sf.beezle.sushi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits strings on a separator. A reverse Joiner. Similar to Google's Splitter
 * (http://guava-libraries.googlecode.com/svn/tags/release09/javadoc/com/google/common/base/Splitter.html), but it's not
 * immutable, configuration uses side-effects. However, once configured, you can use instances concurrently.
 */
public class Splitter {
    public static final Splitter WHITESPACE = Splitter.pattern("\\s+").skipEmpty();
    public static final Splitter SLASH = Splitter.on('/');
    public static final Splitter LIST = Splitter.on(',').trim();

    public static Splitter pattern(String separator) {
        return new Splitter(Pattern.compile(separator, Pattern.MULTILINE), false, false);
    }

    public static Splitter on(char c) {
        return on(Character.toString(c));
    }

    public static Splitter on(String separator) {
        return new Splitter(Pattern.compile(separator, Pattern.MULTILINE | Pattern.LITERAL), false, false);
    }

    //--

    private final Pattern separator;
    private boolean trim;
    private boolean skipEmpty;

    public Splitter(Pattern separator, boolean trim, boolean skipEmpty) {
        if (separator.matcher("").find()) {
            throw new IllegalArgumentException(separator.pattern() + " matches the empty string");
        }
        this.separator = separator;
        this.trim = trim;
        this.skipEmpty = skipEmpty;
    }

    //-- configuration

    public Splitter trim() {
        trim = true;
        return this;
    }

    public Splitter skipEmpty() {
        skipEmpty  = true;
        return this;
    }

    //--

    public List<String> split(String str) {
        List<String> lst;

        lst = new ArrayList<String>();
        split(str, lst);
        return lst;
    }

    public void split(String str, List<String> result) {
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
