package de.ui.sushi;

import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class TestProperties {
    private static Properties singleton;

    public static String get(String key) {
        String result;

        if (singleton == null) {
            try {
                singleton = load();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        result = singleton.getProperty(key);
        if (result == null) {
            throw new IllegalArgumentException(key);
        }
        return result;
    }

    public static Properties load() throws IOException {
        Properties result;
        IO io;
        Node home;
        Node p;
        Reader src;

        result = new Properties();
        io = new IO();
        home = io.guessProjectHome(TestProperties.class);
        src = home.join("test.properties").createReader();
        result.load(src);
        src.close();
        p = home.join("test.properties.private");
        if (p.exists()) {
            src = p.createReader();
            result.load(src);
            src.close();
        }
        return result;
    }
}
