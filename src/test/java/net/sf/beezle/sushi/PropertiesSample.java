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
package net.sf.beezle.sushi;

import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.metadata.Instance;
import net.sf.beezle.sushi.metadata.Type;
import net.sf.beezle.sushi.metadata.reflect.ReflectSchema;

import java.util.Properties;

public class PropertiesSample {
    /** Serialize object to xml and load the result back into an object */
    public static void main(String[] args) {
        Properties props;
        Instance<Obj> data;
        Obj obj;
        
        props = new Properties();
        props.setProperty("foo.number", "2");
        props.setProperty("foo.string", "hi");
        data = TYPE.loadProperties(props, "foo");
        obj = data.get();
        System.out.println("object:\n" + obj);
        obj.number = 3;
        System.out.println("properties:\n" + data.toProperties("bar"));
    }
    
    private static final Type TYPE = new ReflectSchema(new World()).type(Obj.class);
    
    public static class Obj {
        public int number;
        public String string;
        
        public Obj() {
            this(0, "");
        }

        public Obj(int number, String string) {
            this.number = number;
            this.string = string;
        }
        
        @Override
        public String toString() {
            return "number=" + number + ",string=" + string;
        }
    }
}
