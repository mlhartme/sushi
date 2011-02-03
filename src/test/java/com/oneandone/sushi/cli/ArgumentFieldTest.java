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

package de.ui.sushi.cli;

import java.lang.reflect.Field;

import org.junit.Test;

import de.ui.sushi.fs.IO;
import de.ui.sushi.metadata.reflect.ReflectSchema;
import static org.junit.Assert.*;

public class ArgumentFieldTest {
    @Test
    public void integer() {
        check("XY");
    }
    
    private void check(String expected) {
        Argument arg;
        
        arg = ArgumentField.create("fld", new ReflectSchema(new IO()), getField("fld"));
        arg.set(this, expected);
        assertEquals(expected, fld);
    }

    private Field getField(String name) {
        Class<?> c;
        
        c = getClass();
        try {
            return c.getDeclaredField(name);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    //--
    
    private String fld;
}
