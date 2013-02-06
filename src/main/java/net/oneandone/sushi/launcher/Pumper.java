package net.oneandone.sushi.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

public abstract class Pumper extends Thread {
    public static Pumper create(Object streamOrReader, Object streamOrWriter, boolean flushDest, boolean closeDest, String encoding) {
        if ((streamOrReader instanceof InputStream) && (streamOrWriter instanceof OutputStream)) {
            return new BytePumper((InputStream) streamOrReader, (OutputStream) streamOrWriter, flushDest, closeDest);
        }
        if (streamOrWriter instanceof OutputStream) {
            try {
                streamOrWriter = new OutputStreamWriter((OutputStream) streamOrWriter, encoding);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        if (streamOrReader instanceof InputStream) {
            try {
                streamOrReader = new InputStreamReader((InputStream) streamOrReader, encoding);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        return new CharPumper((Reader) streamOrReader, (Writer) streamOrWriter, flushDest, closeDest);
    }

    private Throwable exception;

    public Pumper() {
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            runUnchecked();
        } catch (Throwable e) {
            exception = e;
            return;
        }
    }

    protected abstract void runUnchecked() throws IOException;

    public void finish(Launcher launcher) throws Failure {
        try {
            join();
        } catch (InterruptedException e) {
            throw new Interrupted(e);
        }
        if (exception != null) {
            if (exception instanceof IOException) {
                throw new Failure(launcher, (IOException) exception);
            } else if (exception instanceof Error) {
                throw (Error) exception;
            } else if (exception instanceof RuntimeException) {
                throw (RuntimeException) exception;
            } else {
                throw new IllegalStateException(exception);
            }
        }
    }
}
