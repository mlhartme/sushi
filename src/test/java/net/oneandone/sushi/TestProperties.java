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
package net.oneandone.sushi;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

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
        List<Object[]> objects;

        objects = new ArrayList<>();
        for (String uri : getList(key)) {
            objects.add(new Object[]{uri});
        }
        return objects;
    }


    public static Properties load() throws IOException {
        Properties result;
        World world;
        Node home;
        Node p;

        result = new Properties();
        world = new World();
        home = world.guessProjectHome(TestProperties.class);
        try (Reader src = home.join("test.properties").createReader()) {
            result.load(src);
        }
        p = home.join("test.private.properties");
        if (p.exists()) {
            try (Reader src = p.createReader()) {
                result.load(src);
            }
        }
        return result;
    }
}
