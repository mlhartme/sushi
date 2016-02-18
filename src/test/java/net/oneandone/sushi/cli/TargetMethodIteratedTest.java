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

public class TargetMethodIteratedTest {
    @Test
    public void number() throws NoSuchMethodException {
        Target arg;
        List<Long> lst;

        arg = TargetMethod.create(true, new ReflectSchema(World.createMinimal()), getClass().getMethod("setInt", Long.TYPE));
        lst = new ArrayList<>();
        lst.add((long) 1);
        lst.add((long) 2);
        arg.doSet(this, lst);
        assertEquals(lst, values);
    }

    //--
    
    private List<Long> values = new ArrayList<>();
    
    public void setInt(long l) {
        values.add(l);
    }
}
