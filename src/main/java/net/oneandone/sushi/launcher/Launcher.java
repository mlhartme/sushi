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
package net.oneandone.sushi.launcher;

import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.util.Separator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Configures and executes an operating system process. This class wraps a ProcessBuilder to simplify usage.
 * In particular, most methods return this so you can configure and execute a program in a single expression
 * (short methods names further simplify this). In addition, you can easily get process output as a string.
 *
 * None-zero exit codes of a process are reported as ExitCode exceptions. This helps to improve reliability
 * because it's harder to ignore exceptions than to ignore return codes.
 *
 * Note that the first "arg" passed to an instance of this class is actually not an argument, but
 * the name of the program or script to be executed. I accept this inconsistency because it simplifies
 * the api and allows for shorter method names.
 */
public class Launcher {
    private final ProcessBuilder builder;
    private String encoding;

    public Launcher(String... args) {
        this.builder = new ProcessBuilder();
        arg(args);
    }

    public Launcher(FileNode dir, String... args) {
        this(args);
        dir(dir);
    }

    //-- configuration

    public Launcher env(String key, String value) {
        builder.environment().put(key, value);
        return this;
    }

    public Launcher arg(String... args) {
        for (String a : args) {
            builder.command().add(a);
        }
        return this;
    }

    public Launcher args(List<String> args) {
        builder.command().addAll(args);
        return this;
    }

    /** initializes the directory to execute the command in */
    public Launcher dir(FileNode dir) {
        return dir(dir.toPath().toFile(), dir.getWorld().getSettings().encoding);
    }

    /** You'll normally use the dir(FileNode) method instead. */
    public Launcher dir(File dir, String encoding) {
        this.builder.directory(dir);
        this.encoding = encoding;
        return this;
    }

    //-- execution

    public void execNoOutput() throws Failure {
        String result;

        result = exec();
        if (result.trim().length() > 0) {
            throw new Failure(this, builder.command().get(0) + ": unexpected output " + result);
        }
    }

    public String exec() throws Failure {
        ByteArrayOutputStream result;

        result = new ByteArrayOutputStream();
        exec(result);
        return string(result.toByteArray());
    }

    public void exec(OutputStream all) throws Failure {
        exec(all, null);
    }

    public void exec(OutputStream stdout, OutputStream stderr) throws Failure {
        exec(stdout, stderr, System.in, true);
    }

    /**
     * Executes a command in this directory, wired with the specified streams. None of the argument stream is closed.
     *
     * @param stderr may be null (which will redirect the error stream to stdout.
     * @param stdin may be null
     */
    public void exec(OutputStream stdout, OutputStream stderr, InputStream stdin, boolean stdinInherit) throws Failure {
        Process process;
        int exit;
        String output;
        PumpStream psout;
        PumpStream pserr;
        PumpStream psin;

        if (builder.directory() == null) {
            // builder.start() does not check, I would not detect the problem until process.waitFor is called
            // - that's to late because buffer would also be null
            throw new IllegalStateException("Missing directory. Call dir() before invoking this method");
        }
        builder.redirectErrorStream(stderr == null);
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new Failure(this, e);
        }
        psout = new PumpStream(process.getInputStream(), stdout, false);
        psout.start();
        if (stderr != null) {
            pserr = new PumpStream(process.getErrorStream(), stderr, false);
            pserr.start();
        } else {
            pserr = null;
        }
        if (stdin != null) {
            if (stdinInherit) {
                builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
                psin = null;
            } else {
                psin = new PumpStream(stdin, process.getOutputStream(), true);
                psin.start();
            }
        } else {
            psin = null;
        }
        psout.finish(this);
        if (pserr != null) {
            pserr.finish(this);
        }
        try {
            exit = process.waitFor();
        } catch (InterruptedException e) {
            throw new Interrupted(e);
        }
        if (psin != null) {
            psin.finish(this);
        }
        if (exit != 0) {
            if (stderr == null && stdout instanceof ByteArrayOutputStream) {
                output = string(((ByteArrayOutputStream) stdout).toByteArray());
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
        return "[" + builder.directory() + "] " + Separator.SPACE.join(builder.command());
    }

    //--

    private String string(byte[] bytes) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
