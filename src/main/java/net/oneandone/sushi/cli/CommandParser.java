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
import java.lang.reflect.Parameter;
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

    public static CommandParser create(Schema metadata, Object commandClassOrInstance) {
        return create(metadata, Collections.emptyList(), commandClassOrInstance);
    }

    public static CommandParser create(Schema schema, List<Object> context, Object commandClassOrInstance) {
        Class<?> commandClass;
        CommandParser parser;
        Declaration declaration;
        Option option;
        Value value;
        Command command;

        if (commandClassOrInstance instanceof Class) {
            commandClass = (Class<?>) commandClassOrInstance;
            parser = createParser(schema, commandClass, context);
        } else {
            commandClass = commandClassOrInstance.getClass();
            parser = new CommandParser(commandClassOrInstance);
        }
        for (Object oneContext : context) {
            for (Method m : oneContext.getClass().getMethods()) {
                declaration = Declaration.forAnnotation(m);
                if (declaration != null) {
                    parser.addArgument(ArgumentMethod.create(declaration, schema, oneContext, m));
                }
            }
        }

        for (Method m : commandClass.getMethods()) {
            command = m.getAnnotation(Command.class);
            if (command != null) {
                parser.addCommand(CommandDefinition.create(parser, command.value(), m));
            }
            declaration = Declaration.forAnnotation(m);
            if (declaration != null) {
                parser.addArgument(ArgumentMethod.create(declaration, schema, null, m));
            }
        }
        while (!Object.class.equals(commandClass)) {
            for (Field f: commandClass.getDeclaredFields()) {
                declaration = Declaration.forAnnotation(f);
                if (declaration != null) {
                    parser.addArgument(ArgumentField.create(declaration, schema, f));
                }
            }
            commandClass = commandClass.getSuperclass();
        }
        if (parser.commands.size() == 0) {
            throw new IllegalStateException(commandClass + ": missing command");
        }
        return parser;
    }

    private static CommandParser createParser(Schema schema, Class<?> clazz, List<Object> context) {
        Object[] actuals;
        List<Argument> arguments;
        Constructor found;
        Object[] foundActuals;
        List<Argument> foundArguments;
        CommandParser result;

        found = null;
        foundActuals = null;
        foundArguments = null;
        arguments = new ArrayList<>();
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            arguments.clear();
            actuals = match(schema, constructor, context, arguments);
            if (actuals != null) {
                if (found != null) {
                    throw new IllegalStateException("constructor is ambiguous");
                }
                found = constructor;
                foundActuals = actuals;
                foundArguments = new ArrayList<>(arguments);
            }
        }
        if (found == null) {
            throw new IllegalStateException(clazz + ": no matching constructor");
        }
        result = new CommandParser(found, foundActuals);
        for (Argument a : foundArguments) {
            result.addArgument(a);
        }
        return result;
    }

    private static Object[] match(Schema schema, Constructor constructor, List<Object> context, List<Argument> result) {
        Parameter[] formals;
        Object[] actuals;
        Parameter formal;
        Context ctx;
        Value value;
        Option option;
        Object c;
        int position;
        int currentPosition;
        String name;

        formals = constructor.getParameters();
        actuals = new Object[formals.length];
        position = 1;
        for (int i = 0; i < formals.length; i++) {
            formal = formals[i];
            ctx = formal.getAnnotation(Context.class);
            value = formal.getAnnotation(Value.class);
            option = formal.getAnnotation(Option.class);
            switch (count(ctx, value, option)) {
                case 0:
                    return null; // not fully annotated
                case 1:
                    if (ctx != null) {
                        c = find(context, formal.getType());
                        if (c == null) {
                            throw new IllegalStateException("context object not found: " + formal);
                        }
                        actuals[i] = c;
                    } else if (value != null) {
                        currentPosition = value.position();
                        if (currentPosition == Declaration.POSITION_UNDEFINED) {
                            currentPosition = position;
                        }
                        name = value.value();
                        if (name.equals(Declaration.NAME_UNDEFINED)) {
                            name = formal.getName(); // returns arg<n> if not compiled with "-parameters"
                        }
                        result.add(new ArgumentParameter(
                                new Declaration(currentPosition, name, value.min(), value.max(), value.dflt()),
                                schema.simple(formal.getType()), actuals, i));
                        position++;
                    } else if (option != null) {
                        result.add(new ArgumentParameter(
                                new Declaration(0, option.value(), 0, 1, option.dflt()),
                                schema.simple(formal.getType()), actuals, i));
                    } else {
                        throw new IllegalStateException();
                    }
                    break;
                default:
                    throw new IllegalStateException("duplicate annotations");
            }
        }
        return actuals;
    }

    private static int count(Object ... args) {
        int count;

        count = 0;
        for (Object arg : args) {
            if (arg != null) {
                count++;
            }
        }
        return count;
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

    private final Object commandInstance;
    private final Constructor<?> constructor;
    private final Object[] constructorActuals;
    private final List<CommandDefinition> commands;
    private final Map<String, Argument> options;
    private final List<Argument> values;

    public CommandParser(Object commandInstance) {
        this(commandInstance, null, null);
    }

    public CommandParser(Constructor<?> constructor, Object[] constructorActuals) {
        this(null, constructor, constructorActuals);
    }

    private CommandParser(Object commandInstance, Constructor<?> constructor, Object[] constructorActuals) {
        this.commandInstance = commandInstance;
        this.constructor = constructor;
        this.constructorActuals = constructorActuals;
        this.commands = new ArrayList<>();
        this.options = new HashMap<>();
        this.values = new ArrayList<>();
    }

    public void addCommand(CommandDefinition command) {
        commands.add(command);
    }

    public List<CommandDefinition> getCommands() {
        return commands;
    }

    public void addArgument(Argument arg) {
        Declaration declaration;
        String name;
        int idx;

        declaration = arg.declaration();
        if (declaration.position() == 0) {
            name = declaration.getName();
            if (options.put(name, arg) != null) {
                throw new IllegalArgumentException("duplicate option: " + name);
            }
        } else {
            idx = declaration.position() - 1;
            while (idx >= values.size()) {
                values.add(null);
            }
            if (values.get(idx) != null) {
                throw new IllegalArgumentException("duplicate argument for position " + declaration.position());
            }
            values.set(idx, arg);
        }
    }

    private static boolean isBoolean(Argument arg) {
        return arg.type().getRawType().equals(Boolean.class);
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
        matchArguments(actuals, args);
        actuals.checkCardinality();
        actuals.apply(null);
        target = commandInstance == null ? newInstance() : commandInstance;
        actuals.apply(target);
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
                    StringBuilder builder;

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
