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
package net.oneandone.sushi.cli;

import net.oneandone.sushi.util.Separator;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the part of a command line that comprise one argument.
 */
public class Source {
    public static final String DEFAULT_UNDEFINED = "_default_undefined_";

    public static List<Source> forSyntax(String syntax) {
        int idx;
        String dflt;
        List<Source> result;
        boolean option;
        String name;
        int min;
        int max;
        int last;

        result = new ArrayList<>();
        for (String field : Separator.SPACE.split(syntax)) {
            option = field.startsWith("-");
            if (option) {
                name = field.substring(1);
            } else {
                name = field;
            }
            idx = name.indexOf('=');
            if (idx == -1) {
                dflt = DEFAULT_UNDEFINED;
            } else {
                dflt = name.substring(idx + 1);
                name = name.substring(0, idx);
            }
            last = name.length() -1;
            switch (name.charAt(last)) {
                case '?':
                    min = 0;
                    max = 1;
                    name = name.substring(0, last);
                    break;
                case '*':
                    min = 0;
                    max = Integer.MAX_VALUE;
                    name = name.substring(0, last);
                    break;
                case '+':
                    min = 1;
                    max = Integer.MAX_VALUE;
                    name = name.substring(0, last);
                    break;
                default:
                    min = option ? 0 : 1;
                    max = 1;
                    break;
            }
            result.add(new Source(option, name, min, max, dflt));
        }
        return result;
    }

    //--

    /** 0 for "not positional" - i.e. an option */
    public final boolean option;
    private final String name;
    private final int min;
    private final int max;
    private final String dflt;

    public Source(boolean option, String name, int min, int max, String dflt) {
        if (dflt == null) {
            throw new IllegalArgumentException();
        }
        this.option = option;
        this.name = name;
        this.min = min;
        this.max = max;
        this.dflt = dflt;
    }

    public int max() {
        return max;
    }

    public String getName() {
        return name;
    }

    public void checkCardinality(int size) {
        if (size < min) {
            throw new ArgumentException(name + ": missing value(s). Expected " + min + ", got " + size);
        }
        if (size > max) {
            throw new ArgumentException(name + ": too many values. Expected " + max + ", got " + size);
        }
    }

    public String getDefaultString() {
        return dflt;
    }

    public boolean isOptional() {
        return min == 0;
    }

    public boolean isList() {
        return max > 1;
    }
}
