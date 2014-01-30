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
import net.oneandone.sushi.io.InputLogStream;
import net.oneandone.sushi.io.MultiOutputStream;
import net.oneandone.sushi.io.MultiWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Configurable replacement for System.out, System.err and System.in. 
 * TODO: name clash with java.world.Console in Java 6.
 */
public class Console {
    public static Console create(World world) {
        return new Console(world, new PrintWriter(System.out, true), new PrintWriter(System.err, true), System.in);
    }

    public static Console create(World world, final OutputStream log) {
        return new Console(world,
                new PrintWriter(MultiOutputStream.createTeeStream(System.out, log), true),
                new PrintWriter(MultiOutputStream.createTeeStream(System.err, log), true),
                new InputLogStream(System.in, log));
    }
    
    public final World world;
    public final PrintWriter info;
    public final PrintWriter verbose;
    public final PrintWriter error;
    public final Scanner input;
    
    private final MultiWriter verboseSwitch;
    
    public Console(World world, PrintWriter info, PrintWriter error, InputStream in) {
        this.world = world;
        this.info = info;
        this.verboseSwitch = MultiWriter.createNullWriter();
        this.verbose = new PrintWriter(verboseSwitch, true);
        this.error = error;
        this.input = new Scanner(in);
    }
    
    public boolean getVerbose() {
        return verboseSwitch.dests().size() == 1;
    }
    
    public void setVerbose(boolean verbose) {
        verboseSwitch.dests().clear();
        if (verbose) {
            verboseSwitch.dests().add(info);
        }
    }
    
    public void pressReturn() {
        readline("Press return to continue, ctrl-C to abort.\n");
    }

    public String readline(String message) {
        return readline(message, "");
    }

    public String readline(String message, String dflt) {
        String str;
        
        info.print(message);
        info.flush();
        str = input.nextLine();
        if (str.length() == 0) {
            return dflt;
        } else {
            return str;
        }
    }
}
