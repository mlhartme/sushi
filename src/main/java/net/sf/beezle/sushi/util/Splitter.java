package net.sf.beezle.sushi.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits strings on a separator. A reverse Joiner. Similar to Google's Splitter
 * (http://guava-libraries.googlecode.com/svn/tags/release09/javadoc/com/google/common/base/Splitter.html), but it's not
 * immutable, configuration uses side-effects. However, once configured, you can use instances concurrently.
 */
public class Splitter {
    public static final Splitter SLASH = Splitter.on('/');

    public static Splitter on(char c) {
        return on(Character.toString(c));
    }

    public static Splitter on(String separator) {
        return new Splitter(separator, false);
    }

    //--

    private final String separator;
    private boolean trim;
    private boolean skipEmpty;

    public Splitter(String separator, boolean trim) {
        if (separator.isEmpty()) {
            throw new IllegalArgumentException();
        }
        this.separator = separator;
        this.trim = trim;
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
        int skip;
        int idx;
        int prev;

        if (str.length() > 0) {
            skip = separator.length();
            idx = str.indexOf(separator);
            prev = 0;
            while (idx != -1) {
                add(result, str.substring(prev, idx));
                prev = idx + skip;
                idx = str.indexOf(separator, prev);
            }
            add(result, str.substring(prev));
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
