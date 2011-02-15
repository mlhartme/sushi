package net.sf.beezle.sushi;

import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.World;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestProperties {
    private static Properties singleton;

    public static String getOpt(String key) {
        if (singleton == null) {
            try {
                singleton = load();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return singleton.getProperty(key);
    }

    public static String get(String key) {
        String result;

        result = getOpt(key);
        if (result == null) {
            throw new IllegalArgumentException(key);
        }
        return result;
    }

    public static List<String> getList(String key) {
        List<String> result;
        String value;

        result = new ArrayList<String>();
        for (int i = 1; true; i++) {
            value = getOpt(key + "." + i);
            if (value == null) {
                return result;
            }
            result.add(value);
        }
    }

    public static List<Object[]> getParameterList(String key) {
        List<Object[]> data;

        data = new ArrayList<Object[]>();
        for (String uri : getList(key)) {
            data.add(new Object[] { uri });
        }
        return data;
    }


    public static Properties load() throws IOException {
        Properties result;
        World world;
        Node home;
        Node p;
        Reader src;

        result = new Properties();
        world = new World();
        home = world.guessProjectHome(TestProperties.class);
        src = home.join("test.properties").createReader();
        result.load(src);
        src.close();
        p = home.join("test.private.properties");
        if (p.exists()) {
            src = p.createReader();
            result.load(src);
            src.close();
        }
        return result;
    }
}