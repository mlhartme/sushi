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

    //-- from syntax

    public static CommandParser create(Schema schema, List<Ctx> contexts, String syntax, Class<?> clazz, String mappingString) {
        int idx;
        List<Source> sources;
        String cmd;
        CommandParser parser;
        Mapping mapping;

        mapping = Mapping.parse(mappingString, clazz);
        idx = syntax.indexOf(' ');
        if (idx == -1) {
            cmd = syntax;
            syntax = "";
        } else {
            cmd = syntax.substring(0, idx);
            syntax = syntax.substring(idx + 1);
        }
        sources = Source.forSyntax(syntax);
        parser = createParser(schema, clazz, contexts, sources, mapping);
        parser.addCommand(new CommandDefinition(parser, cmd, commandMethod(clazz, mapping)));
        return parser;
    }

    private static final Class<?>[] NO_ARGS = {};

    private static Method commandMethod(Class<?> clazz, Mapping mapping) {
        String name;

        name = mapping.getCommand();
        if (name == null) {
            name = "run";
        }
        try {
            return clazz.getDeclaredMethod(name, NO_ARGS);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CommandParser createParser(Schema schema, Class<?> clazz, List<Ctx> contexts, List<Source> sources, Mapping mapping) {
        List<Source> constructorSources;
        List<Source> extraSources;
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
        constructorSources = new ArrayList<>(sources.size());
        extraSources = new ArrayList<>();
        for (Source s : sources) {
            if (mapping.contains(s.getName())) {
                extraSources.add(s);
            } else {
                constructorSources.add(s);
            }
        }
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            arguments.clear();
            actuals = match(schema, constructor, contexts, constructorSources, arguments);
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
        for (Source s : extraSources) {
            result.addArgument(new Argument(s, mapping.target(schema, null /* */, s.getName())));
        }
        return result;
    }

    private static Object[] match(Schema schema, Constructor constructor, List<Ctx> initialContexts, List<Source> initialSources,
                                  List<Argument> result) {
        List<Ctx> contexts;
        List<Source> sources;
        Parameter[] formals;
        Object[] actuals;
        Parameter formal;
        Object ctx;
        Source source;

        contexts = new ArrayList<>(initialContexts);
        sources = new ArrayList<>(initialSources);
        formals = constructor.getParameters();
        actuals = new Object[formals.length];
        for (int i = 0; i < formals.length; i++) {
            formal = formals[i];
            ctx = eatContext(contexts, formal.getType());
            if (ctx != null) {
                actuals[i] = ctx;
            } else if (sources.isEmpty()) {
                return null; // too many constructor arguments
            } else {
                source = sources.remove(0);
                result.add(new Argument(source, new TargetParameter(schema, formal.getParameterizedType(), actuals, i)));
            }
        }
        if (!sources.isEmpty()) {
            return null; // not all arguments matched
        }
        return actuals;
    }

    private static Object eatContext(List<Ctx> contexts, Class<?> type) {
        Object obj;

        for (int i = 0, max = contexts.size(); i < max; i++) {
            obj = contexts.get(i).object;
            if (type.isAssignableFrom(obj.getClass())) {
                contexts.remove(i);
                return obj;
            }
        }
        return null;
    }

    //--

    public static CommandParser create(Schema metadata, Object commandClassOrInstance) {
        return create(metadata, Collections.emptyList(), commandClassOrInstance);
    }

    public static CommandParser create(Schema schema, List<Ctx> contexts, Object commandClassOrInstance) {
        Class<?> commandClass;
        CommandParser parser;
        Source source;
        Command command;

        if (commandClassOrInstance instanceof Class) {
            commandClass = (Class<?>) commandClassOrInstance;
            parser = createParser(schema, commandClass, contexts);
        } else {
            commandClass = commandClassOrInstance.getClass();
            parser = new CommandParser(commandClassOrInstance);
        }
        for (Ctx oneContext : contexts) {
            for (Method m : oneContext.object.getClass().getMethods()) {
                source = Source.forAnnotation(m);
                if (source != null) {
                    parser.addArgument(source, TargetMethod.create(schema, oneContext.object, m));
                }
            }
        }

        for (Method m : commandClass.getMethods()) {
            command = m.getAnnotation(Command.class);
            if (command != null) {
                parser.addCommand(CommandDefinition.create(parser, command.value(), m));
            }
            source = Source.forAnnotation(m);
            if (source != null) {
                parser.addArgument(source, TargetMethod.create(schema, null, m));
            }
        }
        while (!Object.class.equals(commandClass)) {
            for (Field f: commandClass.getDeclaredFields()) {
                source = Source.forAnnotation(f);
                if (source != null) {
                    parser.addArgument(source, TargetField.create(schema, f));
                }
            }
            commandClass = commandClass.getSuperclass();
        }
        if (parser.commands.size() == 0) {
            throw new IllegalStateException(commandClass + ": missing command");
        }
        return parser;
    }

    private static CommandParser createParser(Schema schema, Class<?> clazz, List<Ctx> contexts) {
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
            actuals = match(schema, constructor, contexts, arguments);
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

    private static Object[] match(Schema schema, Constructor constructor, List<Ctx> contexts, List<Argument> result) {
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
                        c = find(contexts, formal.getType());
                        if (c == null) {
                            throw new IllegalStateException("context object not found: " + formal);
                        }
                        actuals[i] = c;
                    } else if (value != null) {
                        currentPosition = value.position();
                        if (currentPosition == Source.POSITION_UNDEFINED) {
                            currentPosition = position;
                        }
                        name = value.value();
                        if (name.equals(Source.NAME_UNDEFINED)) {
                            name = formal.getName(); // returns arg<n> if not compiled with "-parameters"
                        }
                        result.add(new Argument(
                                new Source(currentPosition, name, value.min(), value.max(), value.dflt()),
                                new TargetParameter(schema, formal.getParameterizedType(), actuals, i)));
                        position++;
                    } else if (option != null) {
                        result.add(new Argument(
                                new Source(0, option.value(), 0, 1, option.dflt()),
                                new TargetParameter(schema, formal.getParameterizedType(), actuals, i)));
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

    private static Object find(List<Ctx> contexts, Class<?> type) {
        for (Ctx context : contexts) {
            if (type.isAssignableFrom(context.object.getClass())) {
                return context.object;
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

    public void addArgument(Source source, Target target) {
        addArgument(new Argument(source, target));
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
