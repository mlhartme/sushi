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
package net.oneandone.sushi.cli;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class ArgumentFieldTest {
    @Test
    public void integer() {
        check("XY");
    }
    
    private void check(String expected) {
        Argument arg;
        
        arg = ArgumentField.create("fld", new ReflectSchema(new World()), getField("fld"));
        arg.set(this, expected);
        assertEquals(expected, fld);
    }

    private Field getField(String name) {
        Class<?> c;
        
        c = getClass();
        try {
            return c.getDeclaredField(name);
        } catch (SecurityException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    //--
    
    private String fld;
}
