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

import java.io.IOException;
import java.util.Properties;

public class PropertiesSample {
    private static final World world = new World();

    /** Serialize object to xml and load the result back into an object */
    public static void main(String[] args) throws IOException {
        Node propfile;
        Config config;

        propfile = world.getTemp().createTempFile();
        config = new Config();
        config.save(propfile);
        System.out.println("default config created:");
        System.out.println(propfile.readString());

        config.number = 2;
        config.string = "changed";
        config.save(propfile);
        System.out.println("saved changed:");
        System.out.println(propfile.readString());

        config = Config.load(propfile);
        System.out.println("loaded config: " + config);
    }
    
    public static class Config {
        private static final Type TYPE = new ReflectSchema(world).type(Config.class);

        public static Config load(Node file) throws IOException {
            return (Config) TYPE.loadProperties(file.readProperties(), "config").get();
        }

        public int number;
        public String string;
        
        public Config() {
            this(0, "");
        }

        public Config(int number, String string) {
            this.number = number;
            this.string = string;
        }

        public void save(Node file) throws IOException {
            file.writeProperties(TYPE.instance(this).toProperties("config"));
        }

        @Override
        public String toString() {
            return "number=" + number + ",string=" + string;
        }
    }
}
