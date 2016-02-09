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

/** Defines where to store one command line argument (or a list of command line arguments) */
public abstract class Argument {
    private final Declaration declaration;
    private final ArgumentType type;

    protected Argument(Declaration declaration, ArgumentType type) {
        this.declaration = declaration;
        this.type = type;
    }

    public Declaration declaration() {
        return declaration;
    }

    public ArgumentType type() { return type; }
    public abstract boolean before();
    public abstract void set(Object obj, Object value);

    //-- TODO

    public void apply(Object target, List<String> actual) {
        Declaration declaration;
        String d;
        Object converted;

        declaration = declaration();
        if (declaration.isList()) {
            for (String str : actual) {
                try {
                    set(target, type().stringToValue(str));
                } catch (SimpleTypeException e) {
                    throw new ArgumentException("invalid argument " + declaration.getName() + ": " + e.getMessage());
                }
            }
        } else {
            if (actual.isEmpty()) {
                d = declaration.getDefaultString();
                if (Declaration.DEFAULT_UNDEFINED.equals(d)) {
                    converted = type().newInstance();
                } else {
                    try {
                        converted = type().stringToValue(d);
                    } catch (SimpleTypeException e) {
                        throw new IllegalArgumentException("cannot convert default value to type " + type() + ": " + d);
                    }
                }
            } else {
                try {
                    converted = type().stringToValue(actual.get(0));
                } catch (SimpleTypeException e) {
                    throw new ArgumentException("invalid argument " + declaration.getName() + ": " + e.getMessage());
                }
            }
            set(target, converted);
        }
    }

}
