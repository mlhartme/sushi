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
 * A command line parser defined by option, value and child annotations taken from the defining class
 * used to create the parser. The resulting syntax is
 *
 * line = option* child
 *      | option* value1 ... valueN value0*
 *
 * Running the parser configures an instance of the defining class. It takes a command line and an
 * instance of the defining class, options and values perform side effects, children create
 * sub-instances.
 */
public class Cli {
    protected final Console console;
    protected final Schema schema;
    protected boolean exception;

    private final List<CommandMethod> commands;
    private CommandMethod defaultCommand;
    private final List<Object> context;
    private String help;

    public Cli() throws IOException {
        this(World.create());
    }
    
    public Cli(World world) {
        this(Console.create(world));
    }
    
    public Cli(Console console) {
        this(console, new ReflectSchema(console.world));
    }
    
    public Cli(Console console, Schema schema) {
        this.console = console;
        this.schema = schema;
        this.commands = new ArrayList<>();
        this.context = new ArrayList<>();
        this.defaultCommand = null;
        addContext(console);
        addContext(console.world);
        addContext(this);
    }

    public Cli addVersion() {
        addCommand(Version.class);
        return this;
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
            parser = CommandParser.create(schema, command);
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
            this.context.add(obj);
        }
        return this;
    }

    public int run(String... args) {
        return run(Arrays.asList(args));
    }

    public int run(List<String> args) {
        Object[] result;

        try {
            if (commands.size() == 1) {
                result = parseSingle(args);
            } else {
                result = parseNormal(args);
            }
            console.verbose.println("command line: " + args);
            return ((CommandMethod) result[0]).invoke(result[1]);
        } catch (ArgumentException e) {
            console.error.println(e.getMessage());
            console.info.println("Specify 'help' to get a usage message.");
            e.printStackTrace(exception ? console.error : console.verbose);
            return -1;
        } catch (Exception e) {
            console.error.println(e.getMessage());
            e.printStackTrace(exception ? console.error : console.verbose);
            return -1;
        }
    }

    public Object[] parseSingle(List<String> args) {
        CommandMethod c;

        c = commands.get(0);
        return new Object[] { c, c.getParser().run(context, args) };
    }

    public Object[] parseNormal(String... args) {
        return parseNormal(Arrays.asList(args));
    }

    public Object[] parseNormal(List<String> args) {
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
        return new Object[] { c, c.getParser().run(context, lst) };
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

    public static class Version {
        private final Console console;

        public Version(Console console) {
            this.console = console;
        }

        @Command("version")
        public void invoke() {
            Package pkg;

            pkg = getClass().getPackage();
            if (pkg == null) {
                console.info.println("unknown version");
            } else {
                console.info.println(pkg.getName());
                console.info.println("  specification title: " + pkg.getSpecificationTitle());
                console.info.println("  specification version: " + pkg.getSpecificationVersion());
                console.info.println("  specification vendor: " + pkg.getSpecificationVendor());
                console.info.println("  implementation title: " + pkg.getImplementationTitle());
                console.info.println("  implementation version: " + pkg.getImplementationVersion());
                console.info.println("  implementation vendor: " + pkg.getImplementationVendor());
            }
            console.verbose.println("Platform encoding: " + System.getProperty("file.encoding"));
            console.verbose.println("Default Locale: " + Locale.getDefault());
            console.verbose.println("Scanner Locale: " + console.input.locale());
        }
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
