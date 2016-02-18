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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Factory for properly initialized the context object */
public class ContextBuilder {
    public static boolean isOption(String arg) {
        return arg.length() > 1 && arg.startsWith("-");
    }

    //--

    private final ContextBuilder parent;
    private final Object commandInstance;
    private final Constructor<?> constructor;
    private final Object[] constructorActuals;
    private final Map<String, Argument> options;
    private final List<Argument> values;

    public ContextBuilder(ContextBuilder parent, Object commandInstance) {
        this(parent, commandInstance, null, null);
    }

    public ContextBuilder(ContextBuilder parent, Constructor<?> constructor, Object[] constructorActuals) {
        this(parent, null, constructor, constructorActuals);
    }

    private ContextBuilder(ContextBuilder parent, Object commandInstance, Constructor<?> constructor, Object[] constructorActuals) {
        this.parent = parent;
        this.commandInstance = commandInstance;
        this.constructor = constructor;
        this.constructorActuals = constructorActuals;
        this.options = new HashMap<>();
        this.values = new ArrayList<>();
    }

    public void addArgument(Argument arg) {
        Source source;
        String name;
        int idx;

        source = arg.source;
        if (source.position() == 0) {
            name = source.getName();
            if (options.put(name, arg) != null) {
                throw new IllegalArgumentException("duplicate option: " + name);
            }
        } else {
            idx = source.position() - 1;
            while (idx >= values.size()) {
                values.add(null);
            }
            if (values.get(idx) != null) {
                throw new IllegalArgumentException("duplicate argument for position " + source.position());
            }
            values.set(idx, arg);
        }
    }

    /** Convenience for Testing */
    public Object run(String ... args) throws Throwable {
        return run(Arrays.asList(args));
    }

    /** @return Target */
    public Object run(List<String> args) throws Throwable {
        Actuals actuals;
        Object target;

        actuals = new Actuals();
        actuals.defineAll(options.values());
        actuals.defineAll(values);
        fillActuals(args, actuals);
        actuals.save(null);
        target = commandInstance == null ? newInstance() : commandInstance;
        actuals.save(target);
        return target;
    }

    private Object newInstance() throws Throwable {
        try {
            return constructor.newInstance(constructorActuals);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("TODO", e);
        }
    }

    //-- actuals

    private void fillActuals(List<String> args, Actuals actuals) {
        int position;
        String arg;
        Argument argument;
        String value;
        StringBuilder builder;

        position = 0;
        for (int i = 0, max = args.size(); i < max; i++) {
            arg = args.get(i);
            if (isOption(arg)) {
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
                actuals.add(argument, value);
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
                if (actuals.add(argument, value)) {
                    position++;
                }
            }
        }
    }
}
