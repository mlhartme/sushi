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

package net.sf.beezle.sushi.util;

import net.sf.beezle.sushi.fs.Settings;
import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.io.Buffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Configures and executes an operating system process. This class wraps a process builder to simplify usage.
 * In paticular, most methods return this so you can configure and execute a program in a single expression
 * (short methods names further simplify this). In addition, you can easily get program output as a string.
 *
 * Note that the first "arg" passed to an instance of this class is actually not an argument, but
 * the name of the program or script to be executed. I accept this inconsistency because it simplifies
 * the api and allows for shorter method names.
 *
 * None-zero exit codes of a program are reported as ExitCode exceptions. This helps to improve reliability
 * because it's harder to ignore exceptions than to ignore return codes.
 *
 * Currently not supported (because it would need separate threads:
 * feeding input to a process and distinguishing standard and error output.
 */
public class Program {
    private final ProcessBuilder builder;
    private Buffer buffer;
    private Settings settings;

    public Program(String ... args) {
        this.builder = new ProcessBuilder();
        arg(args);
    }

    public Program(FileNode dir, String ... args) {
        this(args);
        dir(dir);
    }

    //-- configuration

    public Program env(String key, String value) {
        builder.environment().put(key, value);
        return this;
    }

    public Program arg(String... args) {
        for (String a : args) {
            builder.command().add(a);
        }
        return this;
    }

    public Program args(List<String> args) {
        builder.command().addAll(args);
        return this;
    }

    /** initializes the directory to execute the command in */
    public Program dir(FileNode dir) {
        return dir(dir.getFile(), dir.getWorld().getBuffer(), dir.getWorld().getSettings());
    }

    /** You'll normally use the dir(FileNode) method instead. */
    public Program dir(File dir, Buffer buffer, Settings settings) {
        this.builder.directory(dir);
        this.buffer = buffer;
        this.settings = settings;
        return this;
    }

    //-- execution

    public void execNoOutput() throws ProgramException {
        String result;

        result = exec();
        if (result.trim().length() > 0) {
            throw new ProgramException(this, builder.command().get(0) + ": unexpected output " + result);
        }
    }

    public String exec() throws ProgramException {
        ByteArrayOutputStream result;

        result = new ByteArrayOutputStream();
        exec(result);
        return settings.string(result.toByteArray());
    }

    /** Executes a command in this directory, returns the output. Core exec method used by all others. */
    public void exec(OutputStream out) throws ProgramException {
        Process process;
        int exit;
        String output;

        if (builder.directory() == null) {
            // builder.start() does not check, I would not detect the problem until process.waitFor is called
            // - that's to late because buffer would also be null
            throw new IllegalStateException("Missing directory. Call dir() before invoking this method");
        }
        builder.redirectErrorStream(true);
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new ProgramException(this, e);
        }
        // because in most cases, buffer is taken from the world and shared with possible other threads
        synchronized (buffer) {
            try {
                buffer.copy(process.getInputStream(), out);
            } catch (IOException e) {
                throw new ProgramException(this, e);
            }
        }
        try {
            exit = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exit != 0) {
            if (out instanceof ByteArrayOutputStream) {
                output = settings.string(((ByteArrayOutputStream) out));
            } else {
                output = "";
            }
            throw new ExitCode(this, exit, output);
        }
    }

    //--

    /** If you need access to the internals. Most applications won't need this method. */
    public ProcessBuilder getBuilder() {
        return builder;
    }

    @Override
    public String toString() {
        return "[" + builder.directory() + "] " + Strings.join(" ", builder.command());
    }
}
