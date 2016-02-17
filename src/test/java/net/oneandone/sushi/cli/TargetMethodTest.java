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

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TargetMethodTest {
    @Test
    public void bool() throws NoSuchMethodException {
        Target arg;

        arg = TargetMethod.create(false, new ReflectSchema(World.createMinimal()), null, getClass().getMethod("bool", Boolean.TYPE));
        arg.doSet(this, true);
        assertEquals(true, this.b);
    }

    @Test
    public void lst() throws NoSuchMethodException {
        Target arg;
        List<Integer> lst;

        lst = new ArrayList<>();
        lst.add(42);
        arg = TargetMethod.create(false, new ReflectSchema(World.createMinimal()), null, getClass().getMethod("lst", List.class));
        arg.doSet(this, lst);
        assertSame(lst, this.lst);
    }

    //--
    
    private boolean b;
    private List<Integer> lst;
    
    public void bool(boolean b) {
        this.b = b;
    }

    public void lst(List<Integer> lst) {
        this.lst = lst;
    }
}
