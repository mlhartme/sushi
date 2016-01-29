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
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandParser {
    public static CommandParser create(Schema metadata, Class<?> commandClass) {
        CommandParser parser;
        Option option;
        Value value;
        Remaining remaining;
        Command command;

        parser = new CommandParser(commandClass);
        for (Method m : commandClass.getMethods()) {
            command = m.getAnnotation(Command.class);
            if (command != null) {
                parser.addCommand(CommandMethod.create(parser, command.value(), m));
            }
            option = m.getAnnotation(Option.class);
            if (option != null) {
                parser.addOption(option.value(), ArgumentMethod.create(option.value(), metadata, m));
            }
            value = m.getAnnotation(Value.class);
            if (value != null) {
                parser.addValue(value.position(), ArgumentMethod.create(value.name(), metadata, m));
            }
            remaining = m.getAnnotation(Remaining.class);
            if (remaining != null) {
                parser.addValue(0, ArgumentMethod.create(remaining.name(), metadata, m));
            }
        }
        while (!Object.class.equals(commandClass)) {
            for (Field f: commandClass.getDeclaredFields()) {
                option = f.getAnnotation(Option.class);
                if (option != null) {
                    parser.addOption(option.value(), ArgumentField.create(option.value(), metadata, f));
                }
                value = f.getAnnotation(Value.class);
                if (value != null) {
                    parser.addValue(value.position(), ArgumentField.create(value.name(), metadata, f));
                }
                remaining = f.getAnnotation(Remaining.class);
                if (remaining != null) {
                    parser.addValue(0, ArgumentField.create(remaining.name(), metadata, f));
                }
            }
            commandClass = commandClass.getSuperclass();
        }
        if (parser.commands.size() == 0) {
            throw new IllegalStateException(commandClass + ": missing command");
        }
        return parser;
    }

    //--

    private final Class<?> clazz;
    private final List<CommandMethod> commands;
    private final Map<String, Argument> options;
    private final List<Argument> values; // and "remaining" at index 0

    public CommandParser(Class<?> clazz) {
        this.clazz = clazz;
        this.commands = new ArrayList<>();
        this.options = new HashMap<>();
        this.values = new ArrayList<>();
        values.add(null);
    }

    public void addCommand(CommandMethod command) {
        commands.add(command);
    }

    public List<CommandMethod> getCommands() {
        return commands;
    }

    public void addOption(String name, Argument arg) {
        if (options.put(name, arg) != null) {
            throw new IllegalArgumentException("duplicate option: " + name);
        }
    }
    
    public void addValue(int position, Argument arg) {
        while (position >= values.size()) {
            values.add(null);
        }
        if (values.get(position) != null) {
            throw new IllegalArgumentException("duplicate argument for position " + position);
        }
        values.set(position, arg);
    }

    private static boolean isBoolean(Argument arg) {
        return arg.getType().getType().equals(Boolean.class);
    }


    /** Convenience for Testing */
    public Object run(String ... args) {
        return run(new ArrayList<>(), Arrays.asList(args));
    }

    /** @return Target */
    public Object run(List<Object> context, List<String> args) {
        int i, max;
        String arg;
        Argument argument;
        String value;
        Object target;

        target = newInstance(context);
        max = args.size();
        for (i = 0; i < max; i++) {
            arg = args.get(i);
            if (arg.length() > 1 && arg.startsWith("-")) {
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
                set(argument, target, value);
            } else {
                break;
            }
        }

        // don't dispatch, target remains unchanged
        for (int position = 1; position < values.size(); position++, i++) {
            if (i >= max) {
                throw new ArgumentException("missing argument '" + values.get(position).getName() + "'");
            }
            set(values.get(position), target, args.get(i));
        }
        if (values.get(0) != null) {
            for ( ; i < max; i++) {
                set(values.get(0), target, args.get(i));
            }
        }
        if (i != max) {
            StringBuilder builder;

            builder = new StringBuilder("unknown value(s):");
            for ( ; i < max; i++) {
                builder.append(' ');
                builder.append(args.get(i));
            }
            throw new ArgumentException(builder.toString());
        }
        return target;
    }

    private Object newInstance(List<Object> context) {
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
            throw new IllegalStateException("no matching constructor");
        }
        try {
            return found.newInstance(arguments);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("TODO", e);
        }
    }

    private Object[] match(Constructor constructor, List<Object> context) {
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

    private Object find(List<Object> context, Class<?> type) {
        for (Object obj : context) {
            if (type.isAssignableFrom(obj.getClass())) {
                return obj;
            }
        }
        return null;
    }

    //--
    
    public void set(Argument arg, Object obj, String value) {
        Object converted;
        
        try {
            converted = run(arg.getType(), value);
        } catch (ArgumentException e) {
            throw new ArgumentException("invalid argument " + arg.getName() + ": " + e.getMessage());
        }
        arg.set(obj, converted);
    }
    
    public Object run(SimpleType simple, String arg) {
        try {
            return simple.stringToValue(arg);
        } catch (SimpleTypeException e) {
            throw new ArgumentException(e.getMessage(), e);
        }
    }
}
