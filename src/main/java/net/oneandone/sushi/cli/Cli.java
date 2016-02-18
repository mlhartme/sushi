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
    public static Cli single(Class<?> command, String syntax) throws IOException {
        Console console;
        Cli cli;

        console = Console.create(World.create());
        cli = new Cli(console.world).begin(console, "-v -e  { setVerbose(v) setStacktraces(e) }");
        cli.command(command, syntax);
        return cli;
    }

    public static Cli create(World world, String help) {
        Console console;
        Cli cli;

        console = Console.create(world);
        cli = new Cli(console.world)
                .begin(console, "-v -e  { setVerbose(v) setStacktraces(e) }")
                   .command(new Help(console, help), "help")
                   .command(PackageVersion.class, "version")
                   .addDefaultCommand("help");
        return cli;
    }

    protected final Schema schema;
    protected boolean exception;

    private final List<Command> commands;
    private Command defaultCommand;
    private Context currentContext;
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
        this.currentContext = null;
        this.defaultCommand = null;
        begin(world);
    }

    public Cli begin(Object context) {
        return begin(context, "");
    }

    public Cli begin(Object context, String syntax) {
        if (context == null) {
            throw new IllegalArgumentException();
        }
        if (context instanceof ExceptionHandler) {
            if (exceptionHandler != null) {
                throw new IllegalStateException("duplicate exception handler: " + exceptionHandler + " vs "+ context);
            }
            exceptionHandler = (ExceptionHandler) context;
        }
        this.currentContext = Context.create(currentContext, context, syntax);
        return this;
    }

    public Cli end() {
        if (currentContext == null) {
            throw new IllegalStateException();
        }
        currentContext = currentContext.parent;
        return this;
    }

    public Cli command(Object clazzOrInstance, String definition) {
        Context context;
        int idx;
        String cmd;
        ContextBuilder builder;

        idx = definition.indexOf(' ');
        if (idx == -1) {
            cmd = definition;
            definition = "";
        } else {
            cmd = definition.substring(0, idx);
            definition = definition.substring(idx + 1);
        }
        context = Context.create(currentContext, clazzOrInstance, definition);
        builder = context.compile(schema);
        if (lookup(cmd) != null) {
            throw new IllegalArgumentException("duplicate command: " + cmd);
        }
        commands.add(new Command(builder, cmd, commandMethod(clazzOrInstance, context.mapping)));
        return this;
    }

    private static final Class<?>[] NO_ARGS = {};

    private static Method commandMethod(Object classOrInstance, Mapping mapping) {
        Class<?> clazz;
        String name;

        name = mapping.getCommand();
        if (name == null) {
            name = "run";
        }
        if (classOrInstance instanceof Class<?>) {
            clazz = (Class<?>) classOrInstance;
        } else {
            clazz = classOrInstance.getClass();
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
        Object obj;
        Command c;
        String name;
        List<String> lst;

        if (exceptionHandler == null) {
            throw new IllegalStateException("missing exception handler");
        }
        try {
            if (commands.size() == 1) {
                c = commands.get(0);
                obj = c.getBuilder().run(args);
            } else {
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
                obj = c.getBuilder().run(lst);
            }
            return c.run(obj);
        } catch (Throwable e) {
            return exceptionHandler.handleException(e);
        }
    }

    private String eatCommand(List<String> args) {
        String arg;

        for (int i = 0, max = args.size(); i < max; i++) {
            arg = args.get(i);
            if (!ContextBuilder.isOption(arg)) {
                args.remove(i);
                return arg;
            }
        }
        return null;
    }

    public Command command(String name) {
        Command result;

        result = lookup(name);
        if (result == null) {
            throw new ArgumentException("command not found: " + name);
        }
        return result;
    }

    public Command lookup(String name) {
        for (Command method : commands) {
            if (name.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

}
