/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.ui.sushi.cli;

import java.util.Arrays;
import java.util.Locale;

import de.ui.sushi.fs.IO;
import de.ui.sushi.metadata.Schema;
import de.ui.sushi.metadata.reflect.ReflectSchema;

/** 
 * Base class for classes with main methods. The command line is defined by annotating
 * derived classes as described for the parser class.
 */
public abstract class Cli {
    protected final Console console;
    protected final Schema schema;
    
    @Option("-pretend")
    protected boolean pretend;

    @Option("v")
    public void setVerbose(boolean v) {
        console.setVerbose(v);
    }

    public Cli() {
        this(new IO());
    }
    
    public Cli(IO io) {
        this(Console.create(io)); 
    }
    
    public Cli(Console console) {
        this(console, new ReflectSchema(console.io)); 
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
            e.printStackTrace(console.verbose);
            return -1;
        } catch (RuntimeException e) {
            console.error.println(e.getMessage());
            e.printStackTrace(console.error);
            return -1;
        } catch (Exception e) {
            console.error.println(e.getMessage());
            e.printStackTrace(console.verbose);
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
