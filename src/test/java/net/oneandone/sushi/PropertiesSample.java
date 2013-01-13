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
import net.oneandone.sushi.metadata.Type;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;

import java.io.IOException;

/**
 * To work with property files, use create a Pojo representing all fields of the file, and use
 * Sushi to map between instances and its file. This exsample demonstrates how to save and load
 * "Config" instances to and from a property file.
 */
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

        config.javaHome = "changed";
        config.version = 2;
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

        public String javaHome;
        public int version;
        public Complex complex;

        public Config() {
            this("", 0, new Complex());
        }

        public Config(String javaHome, int version, Complex complex) {
            this.javaHome = javaHome;
            this.version = version;
            this.complex = complex;
        }

        public void save(Node file) throws IOException {
            file.writeProperties(TYPE.instance(this).toProperties());
        }

        @Override
        public String toString() {
            return "javaHome=" + javaHome + ", version=" + version;
        }
    }

    public static class Complex {
        public int left;
        public int right;
    }
}
