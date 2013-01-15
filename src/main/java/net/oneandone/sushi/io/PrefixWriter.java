package net.oneandone.sushi.io;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

public class PrefixWriter extends FilterWriter {
    private final char newline;
    private final String prefix;
    private boolean start;

    public PrefixWriter(Writer dest, String prefix, char newline) {
        super(dest);
        this.prefix = prefix;
        this.newline = newline;
        this.start = true;
    }

    public void write(int c) throws IOException {
        if (start) {
            out.write(prefix);
            start = false;
        }
        out.write(c);
        if (c == newline) {
            start = true;
        }
    }

    public void write(char cbuf[], int off, int len) throws IOException {
        char c;

        for (int i = 0; i < len; i++) {
            c = cbuf[i + off];
            write(c);
        }
    }

    public void write(String str, int off, int len) throws IOException {
        char c;

        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            write(c);
        }
    }
}
