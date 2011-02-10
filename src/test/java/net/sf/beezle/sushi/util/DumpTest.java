/*
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oneandone.sushi.util;

import com.oneandone.sushi.metadata.model.Car;
import com.oneandone.sushi.metadata.model.Engine;
import com.oneandone.sushi.metadata.model.Vendor;
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
        check("com.oneandone.sushi.util.DumpTest\n", getClass());
    }

    @Test
    public void method() throws Exception {
        check("public void com.oneandone.sushi.util.DumpTest.method() throws java.lang.Exception\n",
                getClass().getDeclaredMethod("method", new Class[] {}));
    }

    private void check(String expected, Object obj) {
        assertEquals(expected, Dump.dump(obj));
    }
}
