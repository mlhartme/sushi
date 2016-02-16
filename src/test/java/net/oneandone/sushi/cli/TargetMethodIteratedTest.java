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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TargetMethodIteratedTest {
    @Test
    public void integer() {
        check(1);
    }
    
    private void check(int expected) {
        Target arg;
        List<Integer> lst;
        
        arg = TargetMethod.create(new ReflectSchema(World.createMinimal()), null, getMethod());
        lst = new ArrayList<>();
        lst.add(expected);
        arg.doSet(this, lst);
        assertEquals(expected, i);
    }

    private Method getMethod() {
        try {
            return getClass().getMethod("setInt", Integer.TYPE);
        } catch (SecurityException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    //--
    
    private Object i;
    
    public void setInt(int i) {
        this.i = i;
    }
}
