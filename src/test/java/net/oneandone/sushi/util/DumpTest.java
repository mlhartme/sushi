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
package net.oneandone.sushi.util;

import net.oneandone.sushi.metadata.model.Car;
import net.oneandone.sushi.metadata.model.Engine;
import net.oneandone.sushi.metadata.model.Vendor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DumpTest {
    @Test
    public void nll() {
        check("null\n", null);
    }
    @Test
    public void string() {
        check("\"str\"\n", "str");
    }
    @Test
    public void object() {
        check("engine\n" +
              "  turbo: false\n" +
              "  ps: 0\n", new Engine());
    }
    @Test
    public void emptyList() {
        check("vendor\n  id: 0\n", new Vendor());
    }
    @Test
    public void list() {
        Vendor v;
        
        v = new Vendor();
        v.cars().add(new Car());
        v.cars().add(new Car());
        check("vendor\n" +
              "  id: 0\n" +
              "  cars[0]: car\n" + 
              "    name: \"\"\n" + 
              "    kind: normal\n" +  
              "    seats: 0\n" +
              "    engine: engine [...]\n" +
              "    radio: null\n" +
              "  cars[1]: car\n" + 
              "    name: \"\"\n" + 
              "    kind: normal\n" +  
              "    seats: 0\n" +
              "    engine: engine [...]\n" +
              "    radio: null\n"
              , 
              v);
    }

    @Test
    public void integer() {
        check("1\n", 1);
    }
    @Test
    public void lng() {
        check("2\n", 2L);
    }
    @Test
    public void character() {
        check("'c'\n", 'c');
    }
    
    @Test
    public void array() {
        check("1\n" +
              "  data[0]: \"2\"\n" +
              "  data[1]: 2\n", 
              new Object() {
                public Object[] data = { "2", 2 };
        });
    }
    
    @Test
    public void clazz() {
        check("net.oneandone.sushi.util.DumpTest\n", getClass());
    }

    @Test
    public void method() throws Exception {
        check("public void net.oneandone.sushi.util.DumpTest.method() throws java.lang.Exception\n",
                getClass().getDeclaredMethod("method", new Class[] {}));
    }

    private void check(String expected, Object obj) {
        assertEquals(expected, Dump.dump(obj));
    }
}
