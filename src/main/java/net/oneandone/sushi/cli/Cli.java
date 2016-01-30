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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A command line parser defined by option and value annotations taken from the command classes.
 * Running the parser instantiates one of the command classes.
 */
public class Cli {
    protected final Schema schema;
    protected boolean exception;

    private final List<CommandMethod> commands;
    private CommandMethod defaultCommand;
    private final List<Object> context;
    private String help;
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
        this.context = new ArrayList<>();
        this.defaultCommand = null;
        addContext(world);
        addContext(this);
    }

    public String getHelp() {
        return help;
    }

    public Cli addHelp(final String str) {
        if (help != null) {
            throw new IllegalStateException();
        }
        help = str;
        addCommand(Help.class);
        return this;
    }

    public Cli addDefaultCommand(String name) {
        defaultCommand = command(name);
        return this;
    }

    public Cli addCommand(Class<?> ... commands) {
        CommandParser parser;

        for (Class<?> command : commands) {
            parser = CommandParser.create(schema, context, command);
            for (CommandMethod method : parser.getCommands()) {
                if (lookup(method.getName()) != null) {
                    throw new IllegalArgumentException("duplicate command: " + method.getName());
                }
                this.commands.add(method);
            }
        }
        return this;
    }

    public Cli addContext(Object ... context) {
        for (Object obj : context) {
            if (obj == null) {
                throw new IllegalArgumentException();
            }
            if (obj instanceof ExceptionHandler) {
                if (exceptionHandler != null) {
                    throw new IllegalStateException("duplicate exception handler: " + exceptionHandler + " vs "+ obj);
                }
                exceptionHandler = (ExceptionHandler) obj;
            }
            this.context.add(obj);
        }
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
            return ((CommandMethod) result[0]).invoke(result[1]);
        } catch (Throwable e) {
            return exceptionHandler.handleException(e);
        }
    }

    public Object[] parseSingle(List<String> args) throws Throwable {
        CommandMethod c;

        c = commands.get(0);
        return new Object[] { c, c.getParser().run(args) };
    }

    public Object[] parseNormal(String... args) throws Throwable {
        return parseNormal(Arrays.asList(args));
    }

    public Object[] parseNormal(List<String> args) throws Throwable {
        CommandMethod c;
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

    public CommandMethod command(String name) {
        CommandMethod result;

        result = lookup(name);
        if (result == null) {
            throw new ArgumentException("command not found: " + name);
        }
        return result;
    }

    public CommandMethod lookup(String name) {
        for (CommandMethod method : commands) {
            if (name.equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    public void setException(boolean exception) {
        this.exception = exception;
    }

    public static class Help {
        private final Cli cli;
        private final Console console;

        public Help(Cli cli, Console console) {
            this.cli = cli;
            this.console = console;
        }

        @Command("help")
        public void invoke() {
            console.info.println(cli.getHelp());
        }
    }
}
