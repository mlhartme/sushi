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
            throw new IllegalStateException("duplicate argument: " + formal);
        }
    }

    public void checkCardinality() {
        for (Map.Entry<Argument, List<String>> entry : actuals.entrySet()) {
            entry.getKey().checkCardinality(entry.getValue().size());
        }
    }

    /** @return true if this formal argument has reached the max number of items. */
    public boolean add(Argument formal, String item) {
        List<String> value;

        value = actuals.get(formal);
        value.add(item);
        return value.size() == formal.max();
    }

    public void apply(Object target) throws SimpleTypeException {
        Argument argument;

        for (Map.Entry<Argument, List<String>> entry : actuals.entrySet()) {
            argument = entry.getKey();
            if (argument.before() == (target == null)) {
                apply(argument, target, entry.getValue());
            }
        }
    }

    private void apply(Argument argument, Object target, List<String> actual) {
        Object converted;

        if (argument.isList()) {
            for (String str : actual) {
                try {
                    argument.set(target, argument.getType().stringToValue(str));
                } catch (SimpleTypeException e) {
                    throw new ArgumentException("invalid argument " + argument.getName() + ": " + e.getMessage());
                }
            }
        } else {
            if (actual.isEmpty()) {
                converted = argument.getDefault();
            } else {
                try {
                    converted = argument.getType().stringToValue(actual.get(0));
                } catch (SimpleTypeException e) {
                    throw new ArgumentException("invalid argument " + argument.getName() + ": " + e.getMessage());
                }
            }
            argument.set(target, converted);
        }
    }
}
