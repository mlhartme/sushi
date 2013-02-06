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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * Configures and executes an operating system process. This class wraps a ProcessBuilder to simplify usage.
 * In particular, most methods return this so you can configure and execute a program in a single expression
 * (short methods names further simplify this). In addition, you can easily get process output as a string.
 *
 * None-zero exit codes of a process are reported as ExitCode exceptions. This helps to improve reliability
 * because it's harder to ignore exceptions than to ignore return codes.
 *
 * Launcher streams chars, although the underlying process stream bytes. Encoding/decoding is performed
 * according to the specified encoding. If you need to stream bytes with Launcher: pass in the appropriate
 * OutputStreamWriter/InputStreamReader and make sure that their encoding and the encoding for Launcher is
 * ascii. (Rationale: character stream are usually more convenient to use in applications than byte streams
 * because process input/output is usually for humans - and thus has to be converted. To keep things simple,
 * I accept the small performance penalty of also de- and encoding; if this is an issue in your application,
 * you have to fall back to Java's ProcessBuilder.)
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
        StringWriter result;

        result = new StringWriter();
        exec(result);
        return result.getBuffer().toString();
    }

    public void exec(Writer all) throws Failure {
        exec(all, null);
    }

    public void exec(Writer stdout, Writer stderr) throws Failure {
        exec(stdout, stderr, true, null, true);
    }

    /**
     * Executes a command in this directory, wired with the specified streams. None of the argument stream is closed.
     *
     * @param stdout never null
     * @param stderr may be null (which will redirect the error stream to stdout.
     * @param flushDest true to flush stdout/stderr after every chunk read from the process
     * @param stdin Has to be null when stdinInherit is true. Otherwise: may be null, which starts a process without input
     */
    public void exec(Writer stdout, Writer stderr, boolean flushDest, Reader stdin, boolean stdinInherit) throws Failure {
        Process process;
        int exit;
        String output;
        Pumper psout;
        Pumper pserr;
        Pumper psin;

        if (builder.directory() == null) {
            // builder.start() does not check, I would not detect the problem until process.waitFor is called
            // - that's to late because buffer would also be null
            throw new IllegalStateException("Missing directory. Call dir() before invoking this method");
        }
        builder.redirectErrorStream(stderr == null);
        if (stdinInherit) {
            builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            if (stdin != null) {
                throw new IllegalArgumentException();
            }
        }
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new Failure(this, e);
        }
        psout = Pumper.create(process.getInputStream(), stdout, flushDest, false, encoding);
        psout.start();
        if (stderr != null) {
            pserr = Pumper.create(process.getErrorStream(), stderr, flushDest, false, encoding);
            pserr.start();
        } else {
            pserr = null;
        }
        if (stdin != null && !stdinInherit) {
            psin = Pumper.create(stdin, process.getOutputStream(), true, true, encoding);
            psin.start();
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
            if (stderr == null && stdout instanceof StringWriter) {
                output = ((StringWriter) stdout).getBuffer().toString();
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
}
