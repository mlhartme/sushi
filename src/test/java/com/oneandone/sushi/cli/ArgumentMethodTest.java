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

package com.oneandone.sushi.cli;

import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.metadata.reflect.ReflectSchema;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class ArgumentMethodTest {
    @Test
    public void integer() {
        check(1);
    }
    
    private void check(int expected) {
        Argument arg;
        
        arg = ArgumentMethod.create("setInt", new ReflectSchema(new IO()), getMethod());
        arg.set(this, expected);
        assertEquals(expected, i);
    }

    private Method getMethod() {
        try {
            return getClass().getMethod("setInt", Integer.TYPE);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    //--
    
    private Object i;
    
    public void setInt(int i) {
        this.i = i;
    }
}
