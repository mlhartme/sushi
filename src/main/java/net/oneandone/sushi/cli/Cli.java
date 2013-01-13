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

import java.util.Arrays;
import java.util.Locale;

/** 
 * Base class for classes with main methods. The command line is defined by annotating
 * derived classes as described for the parser class.
 */
public abstract class Cli {
    protected final Console console;
    protected final Schema schema;
    protected boolean exception;

    @Option("-pretend")
    protected boolean pretend;

    @Option("v")
    public void setVerbose(boolean v) {
        console.setVerbose(v);
    }

    @Option("e")
    public void setException(boolean e) {
        exception = e;
    }

    public Cli() {
        this(new World());
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
    }

    public abstract void printHelp();
    
    public int run(String... args) {
        Parser parser;
        Command command;
        
        parser = Parser.create(schema, getClass());
        try {
            command = (Command) parser.run(this, args);
            console.verbose.println("command line: " + Arrays.asList(args));
            if (pretend) {
                console.info.println("pretend-only, command " + command + " is not executed");
            } else {
                command.invoke();
            }
        } catch (ArgumentException e) {
            console.error.println(e.getMessage());
            console.info.println("Specify 'help' to get a usage message.");
            e.printStackTrace(exception ? console.error : console.verbose);
            return -1;
        } catch (RuntimeException e) {
            console.error.println(e.getMessage());
            e.printStackTrace(console.error);
            return -1;
        } catch (Exception e) {
            console.error.println(e.getMessage());
            e.printStackTrace(exception ? console.error : console.verbose);
            return -1;
        }
        return 0;
    }  
    
    @Child("help")
    public Command help() {
        return new Command() {
            public void invoke() throws Exception {
                printHelp();
            }
        };
    }
    
    @Child("version")
    public Command version() {
        return new Command() {
            public void invoke() throws Exception {
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
        };
    }
}
