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
import net.oneandone.sushi.metadata.Instance;
import net.oneandone.sushi.metadata.Type;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;
import net.oneandone.sushi.metadata.xml.ComplexElement;

import javax.print.attribute.standard.DateTimeAtCompleted;
import java.io.IOException;
import java.util.Properties;

/** Demonstrates how to save and load a class "Config" to and from property files. */
public class PropertiesSample {
    private static final World world = new World();

    /** Serialize object to xml and load the result back into an object */
    public static void main(String[] args) throws IOException {
        Node file;
        Config config;

        file = world.getTemp().createTempFile();
        config = new Config();
        config.save(file);
        System.out.println("default config created:");
        System.out.println(file.readString());

        config.number = 2;
        config.string = "changed";
        config.save(file);
        System.out.println("saved changes:");
        System.out.println(file.readString());

        config = Config.load(file);
        System.out.println("loaded config: " + config);
    }
    
    public static class Config {
        private static final Type TYPE = new ReflectSchema(world).type(Config.class);

        public static Config load(Node file) throws IOException {
            return (Config) TYPE.loadProperties(file.readProperties()).get();
        }

        public int number;
        public String string;
        public Complex complex;

        public Config() {
            this(0, "", new Complex());
        }

        public Config(int number, String string, Complex complex) {
            this.number = number;
            this.string = string;
            this.complex = complex;
        }

        public void save(Node file) throws IOException {
            file.writeProperties(TYPE.instance(this).toProperties());
        }

        @Override
        public String toString() {
            return "number=" + number + ",string=" + string;
        }
    }

    public static class Complex {
        public int left;
        public int right;
    }
}
