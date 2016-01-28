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

    private final List<CommandParser> commands;
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

    public Cli addCommand(Class<?> ... commands) {
        for (Class<?> command : commands) {
            this.commands.add(CommandParser.create(schema, command));
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
            result = parse(args);
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

    public Object[] parse(String... args) {
        return parse(Arrays.asList(args));
    }

    public Object[] parse(List<String> args) {
        CommandMethod c;
        String name;
        List<String> lst;

        if (args.isEmpty()) {
            throw new ArgumentException("missing command");
        }
        name = args.get(0);
        for (CommandParser command : commands) {
            c = command.lookup(name);
            if (c != null) {
                lst = new ArrayList<>(args);
                lst.remove(0);
                return new Object[] { c, command.run(context, lst) };
            }
        }
        throw new ArgumentException("command not found: " + name);
    }

    /* TODO
    @Child("help")
    public Command help() {
        return this::printHelp;
    }
    */

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
