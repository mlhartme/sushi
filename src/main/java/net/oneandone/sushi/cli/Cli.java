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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A command line parser defined by option and value annotations taken from the command classes.
 * Running the parser instantiates one of the command classes.
 */
public class Cli {
    protected final Schema schema;
    protected boolean exception;

    private final List<CommandDefinition> commands;
    private CommandDefinition defaultCommand;
    private final List<Context> contexts;
    private ExceptionHandler exceptionHandler;

    public Cli() throws IOException {
        this(World.create());
    }

    public Cli(World world) {
        this(world, new ReflectSchema(world));
    }
    
    public Cli(World world, Schema schema) {
        this.schema = schema;
        this.commands = new ArrayList<>();
        this.contexts = new ArrayList<>();
        this.defaultCommand = null;
        context(world);
    }

    public Cli context(Object context) {
        return context(context, "");
    }

    public Cli context(Object context, String syntax) {
        if (context == null) {
            throw new IllegalArgumentException();
        }
        if (context instanceof ExceptionHandler) {
            if (exceptionHandler != null) {
                throw new IllegalStateException("duplicate exception handler: " + exceptionHandler + " vs "+ context);
            }
            exceptionHandler = (ExceptionHandler) context;
        }
        this.contexts.add(Context.create(context, syntax));
        return this;
    }

    public Cli command(String syntax, Class<?> clazz) {
        CommandParser parser;

        parser = create(syntax, clazz);
        for (CommandDefinition method : parser.getCommands()) {
            if (lookup(method.getName()) != null) {
                throw new IllegalArgumentException("duplicate command: " + method.getName());
            }
            this.commands.add(method);
        }
        return this;
    }

    private CommandParser create(String syntax, Class<?> clazz) {
        Context context;
        int idx;
        String cmd;
        CommandParser parser;

        idx = syntax.indexOf(' ');
        if (idx == -1) {
            cmd = syntax;
            syntax = "";
        } else {
            cmd = syntax.substring(0, idx);
            syntax = syntax.substring(idx + 1);
        }
        context = Context.create(clazz, syntax);
        parser = context.createParser(schema, contexts);
        parser.addCommand(new CommandDefinition(parser, cmd, commandMethod(clazz, context.mapping)));
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

    public Cli addDefaultCommand(String name) {
        defaultCommand = command(name);
        return this;
    }

    public int run(String... args) {
        return run(Arrays.asList(args));
    }

    public int run(List<String> args) {
        Object[] result;

        if (exceptionHandler == null) {
            throw new IllegalStateException("missing exception handler");
        }
        try {
            if (commands.size() == 1) {
                result = parseSingle(args);
            } else {
                result = parseNormal(args);
            }
            return ((CommandDefinition) result[0]).invoke(result[1]);
        } catch (Throwable e) {
            return exceptionHandler.handleException(e);
        }
    }

    public Object[] parseSingle(List<String> args) throws Throwable {
        CommandDefinition c;

        c = commands.get(0);
        return new Object[] { c, c.getParser().run(args) };
    }

    public Object[] parseNormal(String... args) throws Throwable {
        return parseNormal(Arrays.asList(args));
    }

    public Object[] parseNormal(List<String> args) throws Throwable {
        CommandDefinition c;
        String name;
        List<String> lst;

        lst = new ArrayList<>(args);
        name = eatCommand(lst);
        if (name == null) {
            c = defaultCommand;
            if (c == null) {
                throw new ArgumentException("missing command");
            }
        } else {
            c = command(name);
        }
        return new Object[] { c, c.getParser().run(lst) };
    }

    private String eatCommand(List<String> args) {
        String arg;

        for (int i = 0, max = args.size(); i < max; i++) {
            arg = args.get(i);
            if (!CommandParser.isOption(arg)) {
                args.remove(i);
                return arg;
            }
        }
        return null;
    }

    public CommandDefinition command(String name) {
        CommandDefinition result;

        result = lookup(name);
        if (result == null) {
            throw new ArgumentException("command not found: " + name);
        }
        return result;
    }

    public CommandDefinition lookup(String name) {
        for (CommandDefinition method : commands) {
            if (name.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

}
