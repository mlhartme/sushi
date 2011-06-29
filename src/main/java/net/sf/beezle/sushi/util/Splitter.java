package net.sf.beezle.sushi.util;

import java.util.ArrayList;
import java.util.List;

public class Splitter {
    public static final Splitter SLASH = Splitter.on('/');

    public static Splitter on(char c) {
        return on(Character.toString(c));
    }

    public static Splitter on(String separator) {
        return new Splitter(separator);
    }

    //--

    private final String separator;

    public Splitter(String separator) {
        this.separator = separator;
    }

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

        if (separator.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (str.length() > 0) {
            skip = separator.length();
            idx = str.indexOf(separator);
            prev = 0;
            while (idx != -1) {
                result.add(str.substring(prev, idx));
                prev = idx + skip;
                idx = str.indexOf(separator, prev);
            }
            result.add(str.substring(prev));
        }
    }

}
