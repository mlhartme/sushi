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

import net.oneandone.sushi.metadata.Schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandParser {
    public static boolean isOption(String arg) {
        return arg.length() > 1 && arg.startsWith("-");
    }

    public static CommandParser create(Schema metadata, Class<?> commandClass) {
        return create(metadata, Collections.emptyList(), commandClass);
    }

    public static CommandParser create(Schema metadata, List<Object> context, Class<?> commandClass) {
        CommandParser parser;
        Option option;
        Value value;
        Command command;

        parser = createParser(commandClass, context);
        for (Object oneContext : parser.context) {
            for (Method m : oneContext.getClass().getMethods()) {
                option = m.getAnnotation(Option.class);
                if (option != null) {
                    parser.addOption(option.value(), ArgumentMethod.create(option.value(), metadata, 0, 1, oneContext, m));
                }
            }
        }

        for (Method m : commandClass.getMethods()) {
            command = m.getAnnotation(Command.class);
            if (command != null) {
                parser.addCommand(CommandDefinition.create(parser, command.value(), m));
            }
            option = m.getAnnotation(Option.class);
            if (option != null) {
                parser.addOption(option.value(), ArgumentMethod.create(option.value(), metadata, 0, 1, null, m));
            }
            value = m.getAnnotation(Value.class);
            if (value != null) {
                parser.addValue(value.position(), ArgumentMethod.create(value.name(), metadata, value.min(), value.max(), null, m));
            }
        }
        while (!Object.class.equals(commandClass)) {
            for (Field f: commandClass.getDeclaredFields()) {
                option = f.getAnnotation(Option.class);
                if (option != null) {
                    parser.addOption(option.value(), ArgumentField.create(option.value(), metadata, 0, 1, f));
                }
                value = f.getAnnotation(Value.class);
                if (value != null) {
                    parser.addValue(value.position(), ArgumentField.create(value.name(), metadata, value.min(), value.max(), f));
                }
            }
            commandClass = commandClass.getSuperclass();
        }
        if (parser.commands.size() == 0) {
            throw new IllegalStateException(commandClass + ": missing command");
        }
        return parser;
    }

    private static CommandParser createParser(Class<?> clazz, List<Object> context) {
        Object[] candidate;
        Constructor found;
        Object[] arguments;

        found = null;
        arguments = null;
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            candidate = match(constructor, context);
            if (candidate != null) {
                if (found != null) {
                    throw new IllegalStateException("constructor is ambiguous");
                }
                found = constructor;
                arguments = candidate;
            }
        }
        if (found == null) {
            throw new IllegalStateException(clazz + ": no matching constructor");
        }
        return new CommandParser(found, arguments);
    }

    private static Object[] match(Constructor constructor, List<Object> context) {
        Class<?>[] formals;
        Object[] actuals;

        formals = constructor.getParameterTypes();
        actuals = new Object[formals.length];
        for (int i = 0; i < formals.length; i++) {
            if ((actuals[i] = find(context, formals[i])) == null) {
                return null;
            }
        }
        return actuals;
    }

    private static Object find(List<Object> context, Class<?> type) {
        for (Object obj : context) {
            if (type.isAssignableFrom(obj.getClass())) {
                return obj;
            }
        }
        return null;
    }

    //--

    private final Constructor<?> constructor;
    private final Object[] context;
    private final List<CommandDefinition> commands;
    private final Map<String, Argument> options;
    private final List<Argument> values;
    private Argument remainingValues;

    public CommandParser(Constructor<?> constructor, Object[] context) {
        this.constructor = constructor;
        this.context = context;
        this.commands = new ArrayList<>();
        this.options = new HashMap<>();
        this.values = new ArrayList<>();
        this.remainingValues = null;
    }

    public void addCommand(CommandDefinition command) {
        commands.add(command);
    }

    public List<CommandDefinition> getCommands() {
        return commands;
    }

    public void addOption(String name, Argument arg) {
        if (options.put(name, arg) != null) {
            throw new IllegalArgumentException("duplicate option: " + name);
        }
    }
    
    public void addValue(int position, Argument arg) {
        int idx;

        idx = position - 1;
        while (idx >= values.size()) {
            values.add(null);
        }
        if (values.get(idx) != null) {
            throw new IllegalArgumentException("duplicate argument for position " + position);
        }
        values.set(idx, arg);
    }

    public void addRemaining(Argument arg) {
        if (remainingValues != null) {
            throw new IllegalArgumentException("too many remaining arguments");
        }
        remainingValues = arg;
    }

    private static boolean isBoolean(Argument arg) {
        return arg.getType().getType().equals(Boolean.class);
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
        if (remainingValues != null) {
            actuals.define(remainingValues);
        }
        matchArguments(actuals, args);
        actuals.checkCardinality();
        actuals.apply(null);
        target = newInstance();
        actuals.apply(target);
        return target;
    }

    private Object newInstance() throws Throwable {
        try {
            return constructor.newInstance(context);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("TODO", e);
        }
    }

    //-- actuals

    private void matchArguments(Actuals actuals, List<String> args) {
        int position;
        String arg;
        Argument argument;
        String value;

        position = 0;
        for (int i = 0, max = args.size(); i < max; i++) {
            arg = args.get(i);
            if (isOption(arg)) {
                argument = options.get(arg.substring(1));
                if (argument == null) {
                    throw new ArgumentException("unknown option " + arg);
                }
                if (isBoolean(argument)) {
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
                    argument = remainingValues;
                    if (argument == null) {
                        StringBuilder builder;

                        builder = new StringBuilder("unknown value(s):");
                        for ( ; i < max; i++) {
                            builder.append(' ');
                            builder.append(args.get(i));
                        }
                        throw new ArgumentException(builder.toString());
                    }
                }
                value = arg;
                if (actuals.add(argument, value)) {
                    position++;
                }
            }
        }
    }
}
