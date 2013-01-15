package net.oneandone.sushi.io;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class PrefixWriterTest {
    @Test
    public void test() throws IOException {
        check("");
        check("", "");
        check("-a", "a");
        check("-ab", "a", "b");
        check("-a\n", "a\n");
        check("-a\n-bc", "a\nbc");
        check("-1\n-2\n-3\n", "1\n", "2\n", "3\n");
    }

    private void check(String expected, String ... args) throws IOException {
        StringWriter dest;
        PrefixWriter pw;

        dest = new StringWriter();
        pw = new PrefixWriter(dest, "-", '\n');
        for (String arg : args) {
            pw.write(arg);
        }
        assertEquals(expected, dest.toString());
    }
}
