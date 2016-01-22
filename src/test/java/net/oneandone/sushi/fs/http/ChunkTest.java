package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.fs.http.io.AsciiInputStream;
import net.oneandone.sushi.fs.http.io.AsciiOutputStream;
import net.oneandone.sushi.fs.http.io.ChunkedInputStream;
import net.oneandone.sushi.fs.http.io.ChunkedOutputStream;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ChunkTest {
    @Test
    public void normal() throws IOException {
        int c;
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        ChunkedOutputStream dest = new ChunkedOutputStream(5, new AsciiOutputStream(data, 9));
        dest.write("hello, world\n".getBytes());
        dest.write("second line".getBytes());
        dest.close();
        ChunkedInputStream src = new ChunkedInputStream(new AsciiInputStream(new ByteArrayInputStream(data.toByteArray()), 10));
        data = new ByteArrayOutputStream();
        while (true) {
            c = src.read();
            if (c == -1) {
                break;
            }
            data.write(c);
        }
        assertEquals("hello, world\nsecond line", new String(data.toByteArray()));
    }
}
