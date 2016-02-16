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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Defines the part of a command line that comprise one argument.
 * Value, Option and Remaining annotations result in Source objects */
public class Source {
    public static final int POSITION_UNDEFINED = Integer.MIN_VALUE;
    public static final String NAME_UNDEFINED = "_name_undefined_";
    public static final String DEFAULT_UNDEFINED = "_default_undefined_";

    public static Source forAnnotation(AnnotatedElement element) {
        Source result;

        result = null;
        for (Annotation a : element.getAnnotations()) {
            if (a instanceof Option) {
                result = merge(result, toSource((Option) a));
            } else if (a instanceof Value) {
                result = merge(result, toSource((Value) a));
            } else if (a instanceof Remaining) {
                result = merge(result, toSource((Remaining) a));
            }
        }
        return result;
    }

    private static Source merge(Source prev, Source next) {
        if (prev != null) {
            throw new IllegalArgumentException("ambiguous annotations: " + prev + " vs " + next);
        }
        return next;
    }

    public static Source toSource(Value value) {
        return new Source(value.position(), value.value(), value.min(), value.max(), value.dflt());
    }

    public static Source toSource(Option option) {
        return new Source(0, option.value(), 0, 1, option.dflt());
    }

    public static Source toSource(Remaining remaining) {
        return new Source(remaining.position(), remaining.value(), 0, Integer.MAX_VALUE, DEFAULT_UNDEFINED);
    }

    //--

    /** 0 for "not positional" - i.e. an option */
    private final int position;
    private final String name;
    private final int min;
    private final int max;
    private final String dflt;

    public Source(int position, String name, int min, int max, String dflt) {
        if (dflt == null) {
            throw new IllegalArgumentException();
        }
        this.position = position;
        this.name = name;
        this.min = min;
        this.max = max;
        this.dflt = dflt;
    }

    public int position() {
        return position;
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
