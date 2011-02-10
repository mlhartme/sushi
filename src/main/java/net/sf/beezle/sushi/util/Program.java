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

import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.fs.file.FileNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Wraps a Process builder to add some convenience methods
 */
public class Program {
    public final World world;
    private final FileNode dir;
    public final ProcessBuilder builder;
    
    public Program(FileNode dir, String ... args) {
        this.world = dir.getWorld();
        this.dir = dir;
        this.builder = new ProcessBuilder();
        this.builder.directory(dir.getFile());
        add(args);
    }
        
    public Program add(String ... args) {
        for (String a : args) {
            builder.command().add(a);
        }
        return this;
    }
    
    public Program addAll(List<String> args) {
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
        return world.getSettings().string(result.toByteArray());
    }
    
    /** executes a command in this directory, returns the output */
    public void exec(OutputStream dest) throws IOException {
        Process process;
        int exit;
        String output;
        
        builder.redirectErrorStream(true);
        process = builder.start();
        // this looks like a busy wait to me, but it's what all the examples suggest:
        world.getBuffer().copy(process.getInputStream(), dest);
        try {
            exit = process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (exit != 0) {
            if (dest instanceof ByteArrayOutputStream) {
                output = world.getSettings().string(((ByteArrayOutputStream) dest));
            } else {
                output = "";
            }
            throw new ExitCode(builder.command(), exit, output);
        }
    }

    @Override
    public String toString() {
        return "[" + dir + "] " + Strings.join(" ", builder.command());
    }
}
