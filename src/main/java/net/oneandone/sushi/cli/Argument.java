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

import net.oneandone.sushi.metadata.SimpleTypeException;

import java.util.List;

/** Associates a source with a target. */
public class Argument {
    public final Source source;
    public final Target target; // type of the argument/field where to store

    public Argument(Source source, Target target) {
        this.source = source;
        this.target = target;
    }

    public void set(Object dest, List<String> actual) {
        String d;
        Object converted;

        if (source.isList()) {
            for (String str : actual) {
                try {
                    target.doSet(dest, target.stringToComponent(str));
                } catch (SimpleTypeException e) {
                    throw new ArgumentException("invalid argument " + source.getName() + ": " + e.getMessage());
                }
            }
        } else {
            if (actual.isEmpty()) {
                d = source.getDefaultString();
                if (Source.DEFAULT_UNDEFINED.equals(d)) {
                    converted = target.newComponent();
                } else {
                    try {
                        converted = target.stringToComponent(d);
                    } catch (SimpleTypeException e) {
                        throw new IllegalArgumentException("cannot convert default value to type " + target + ": " + d);
                    }
                }
            } else {
                try {
                    converted = target.stringToComponent(actual.get(0));
                } catch (SimpleTypeException e) {
                    throw new ArgumentException("invalid argument " + source.getName() + ": " + e.getMessage());
                }
            }
            target.doSet(dest, converted);
        }
    }
}
