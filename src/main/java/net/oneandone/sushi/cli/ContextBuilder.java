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

    private final Context context;
    private final ContextBuilder parent;
    private final Object commandInstance;
    private final Constructor<?> constructor;
    private final Object[] constructorActuals;
    private final Map<String, Argument> options;
    private final List<Argument> values;

    public ContextBuilder(Context context, ContextBuilder parent, Object commandInstance) {
        this(context, parent, commandInstance, null, null);
    }

    public ContextBuilder(Context context, ContextBuilder parent, Constructor<?> constructor, Object[] constructorActuals) {
        this(context, parent, null, constructor, constructorActuals);
    }

    private ContextBuilder(Context context, ContextBuilder parent, Object commandInstance, Constructor<?> constructor, Object[] constructorActuals) {
        this.context = context;
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

    //--

    /** Convenience for Testing */
    public Object run(String ... args) throws Throwable {
        return run(Arrays.asList(args));
    }

    /** @return Target */
    public Object run(List<String> args) throws Throwable {
        Actuals actuals;
        Map<String, Argument> allOptions;
        List<Argument> allValues;
        Map<Context, Object> context;

        actuals = new Actuals();
        define(actuals);
        allOptions = new HashMap<>();
        addOptions(allOptions);
        allValues = new ArrayList<>();
        addValues(allValues);
        actuals.fill(args, allOptions, allValues);
        return instantiate(actuals, new HashMap<>());
    }

    private Object instantiate(Actuals actuals, Map<Context, Object> instantiatedContexts) throws Throwable {
        Object obj;

        if (parent != null) {
            parent.instantiate(actuals, instantiatedContexts);
        }
        actuals.save(context, null);
        obj = commandInstance == null ? newInstance(instantiatedContexts) : commandInstance;
        actuals.save(context, obj);
        return obj;
    }

    private void define(Actuals result) {
        if (parent != null) {
            parent.define(result);
        }
        result.defineAll(options.values());
        result.defineAll(values);
    }

    private Object newInstance(Map<Context, Object> instantiatedContexts) throws Throwable {
        Object instance;

        for (int i = 0, max = constructorActuals.length; i < max; i++) {
            if (constructorActuals[i] instanceof Context) {
                instance = instantiatedContexts.get(constructorActuals[i]);
                if (instance == null) {
                    throw new IllegalStateException();
                }
                constructorActuals[i] = instance;
            }
        }
        try {
            instance = constructor.newInstance(constructorActuals);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("TODO", e);
        }
        instantiatedContexts.put(context, instance);
        return instance;
    }

    private void addOptions(Map<String, Argument> result) {
        if (parent != null) {
            parent.addOptions(result);
        }
        result.putAll(options);
    }

    private void addValues(List<Argument> result) {
        if (parent != null) {
            parent.addValues(result);
        }
        result.addAll(values);
    }
}
