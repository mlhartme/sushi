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

package com.oneandone.sushi.cli;

import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.io.InputLogStream;
import com.oneandone.sushi.io.MultiOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * Configurable replacement for System.out, System.err and System.in. 
 * TODO: name clash with java.io.Console in Java 6. 
 */
public class Console {
    public static Console create(IO io) {
        return new Console(io, System.out, System.err, System.in);
    }

    public static Console create(IO io, final OutputStream log) {
        return new Console(io, 
                new PrintStream(MultiOutputStream.createTeeStream(System.out, log), true), 
                new PrintStream(MultiOutputStream.createTeeStream(System.err, log), true), 
                new InputLogStream(System.in, log));
    }
    
    public final IO io;
    public final PrintStream info;
    public final PrintStream verbose;
    public final PrintStream error;
    public final Scanner input;
    
    private final MultiOutputStream verboseSwitch;
    
    public Console(IO io, PrintStream info, PrintStream error, InputStream in) {
        this.io = io;
        this.info = info;
        this.verboseSwitch = MultiOutputStream.createNullStream();
        this.verbose = new PrintStream(verboseSwitch);
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
    
    public void pressReturn() throws IOException {
        readline("Press return to continue, ctrl-C to abort.\n");
    }

    public String readline(String message) throws IOException {
        return readline(message, "");
    }

    public String readline(String message, String dflt) throws IOException {
        String str;
        
        info.print(message);
        str = input.nextLine();
        if (str.length() == 0) {
            return dflt;
        } else {
            return str;
        }
    }
}
