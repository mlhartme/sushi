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
 * Wraps a Process builder to add some convenience methods.
 */
public class Program {
    private final ProcessBuilder builder;
    private Buffer buffer;
    private Settings settings;

    public Program(FileNode dir, String ... args) {
        this(args);
        dir(dir);
    }

    public Program(String ... args) {
        this.builder = new ProcessBuilder();
        arg(args);
    }

    public ProcessBuilder getBuilder() {
        return builder;
    }

    //--

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
    
    public void execNoOutput() throws IOException {
        String result;
        
        result = exec();
        if (result.trim().length() > 0) {
            throw new IOException(builder.command().get(0) + ": unexpected output " + result);
        }
    }
    
    public String exec() throws IOException {
        ByteArrayOutputStream result;
        
        result = new ByteArrayOutputStream();
        exec(result);
        return settings.string(result.toByteArray());
    }
    
    /** Executes a command in this directory, returns the output. Core exec method used by all others. */
    public void exec(OutputStream dest) throws IOException {
        Process process;
        int exit;
        String output;
        
        builder.redirectErrorStream(true);
        process = builder.start();
        synchronized (buffer) {
            // this looks like a busy wait to me, but it's what all the examples suggest:
            buffer.copy(process.getInputStream(), dest);
        }
        try {
            exit = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exit != 0) {
            if (dest instanceof ByteArrayOutputStream) {
                output = settings.string(((ByteArrayOutputStream) dest));
            } else {
                output = "";
            }
            throw new ExitCode(builder.command(), exit, output);
        }
    }

    @Override
    public String toString() {
        return "[" + builder.directory() + "] " + Strings.join(" ", builder.command());
    }
}
