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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Maps formals to actuals */
public class Actuals {
    private final Map<Argument, List<String>> actuals;

    public Actuals() {
        this.actuals = new HashMap<>();
    }

    public void defineAll(Collection<Argument> formals) {
        for (Argument formal : formals) {
            define(formal);
        }
    }
    public void define(Argument formal) {
        if (actuals.put(formal, new ArrayList<>()) != null) {
            throw new InvalidCliException("duplicate argument: " + formal);
        }
    }

    /** @return true if this formal argument has reached the max number of items. */
    public boolean add(Argument formal, String item) {
        List<String> value;

        value = actuals.get(formal);
        value.add(item);
        return value.size() == formal.source.max();
    }

    public void save(Context context, Object target) throws SimpleTypeException {
        Argument argument;

        for (Map.Entry<Argument, List<String>> entry : actuals.entrySet()) {
            argument = entry.getKey();
            if (argument.context == context) {
                if (argument.target.before() == (target == null)) {
                    entry.getKey().source.checkCardinality(entry.getValue().size());
                    argument.set(target, entry.getValue());
                }
            }
        }
    }

    public void fill(List<String> args, Map<String, Argument> options, List<Argument> values) {
        int position;
        String arg;
        Argument argument;
        String value;
        StringBuilder builder;

        position = 0;
        for (int i = 0, max = args.size(); i < max; i++) {
            arg = args.get(i);
            if (ContextBuilder.isOption(arg)) {
                argument = options.get(arg.substring(1));
                if (argument == null) {
                    throw new ArgumentException("unknown option " + arg);
                }
                if (argument.target.isBoolean()) {
                    value = "true";
                } else {
                    if (i + 1 >= max) {
                        throw new ArgumentException("missing value for option " + arg);
                    }
                    i++;
                    value = args.get(i);
                }
                add(argument, value);
            } else {
                if (position < values.size()) {
                    argument = values.get(position);
                } else {
                    builder = new StringBuilder("unknown value(s):");
                    for ( ; i < max; i++) {
                        builder.append(' ');
                        builder.append(args.get(i));
                    }
                    throw new ArgumentException(builder.toString());
                }
                value = arg;
                if (add(argument, value)) {
                    position++;
                }
            }
        }

    }
}
