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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.Instance;
import net.oneandone.sushi.metadata.Type;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;

public class MetadataSample {
    private static final World WORLD = new World();
    /** Serialize object to xml and load the result back into an object */
    public static void main(String[] args) throws Exception {
        Instance<Obj> instance;
        Obj obj;
        
        instance = TYPE.loadXml(WORLD.memoryNode("<obj><number>2</number><string>str</string></obj>"));
        obj = instance.get();
        System.out.println("object:\n" + obj);
        obj.number = 3;
        System.out.println("xml:\n" + instance.toXml());
    }
    
    private static final Type TYPE = new ReflectSchema(WORLD).type(Obj.class);
    
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
