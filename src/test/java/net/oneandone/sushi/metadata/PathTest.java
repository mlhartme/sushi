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
package net.oneandone.sushi.metadata;

import net.oneandone.sushi.metadata.Path.Step;
import net.oneandone.sushi.metadata.model.Car;
import net.oneandone.sushi.metadata.model.ModelBase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PathTest extends ModelBase {
    @Test
    public void parse() {
        List<Step> steps;

        steps = new Path("foo/bar[2]").steps();
        assertEquals(2, steps.size());
        assertEquals("foo", steps.get(0).name);
        assertEquals(-1, steps.get(0).idx);
        assertEquals("bar", steps.get(1).name);
        assertEquals(2, steps.get(1).idx);
    }

    @Test
    public void empty() {
        one(audi, audi, "");
    }

    @Test
    public void selectOne() {
        one("audi", audi, "name");
        one(4, audi, "seats");
        one(false, audi, "engine/turbo");
        one(90, audi, "engine/ps");
        one(audi.getRadio(), audi, "radio");
    }

    @Test
    public void selectAll() {
        all(vendor, "car", audi, bmw);
        all(vendor, "car/engine/ps", 90, 200);
        all(vendor, "car/radio", audi.getRadio());
    }

    @Test
    public void selectIndex() {
        all(vendor, "car[0]", audi);
        all(vendor, "car[1]", bmw);
        all(vendor, "car[1]/engine", bmw.getEngine());
    }

    @Test
    public void accessFalse() {
        Variable<Integer> v;

        assertTrue(null, access(bmw, "radio", false).get().isEmpty());
        assertSame(audi.getRadio(), access(audi, "radio", false).getOne());

        v = (Variable) access(audi, "engine/ps", false);
        assertEquals((Object) 90, v.getOne());
        v.set(91);
        assertEquals((Object) 91, v.getOne());
    }

    @Test
    public void accessAmbiguous() {
        try {
            access(vendor, "car/engine", false);
            fail();
        } catch (PathException e) {
            // ok
        }
    }

    @Test
    public void accessTrue() {
        Variable<Boolean> v;

        assertNull(bmw.getRadio());
        v = (Variable) access(bmw, "radio/cd", true);
        assertNotNull(bmw.getRadio());
        assertFalse(v.getOne());
        v.set(true);
        assertTrue(bmw.getRadio().getCd());
    }

    @Test
    public void accessIndex() {
        Variable<?> v;

        assertEquals(2, vendor.cars().size());
        v = access(vendor, "car[3]/engine", true);
        assertEquals(4, vendor.cars().size());
        assertSame(((Car) vendor.cars().get(3)).getEngine(), v.getOne());
    }

    @Test
    public void accessLastIndex() {
        try {
            access(vendor, "car[3]", true);
            fail();
        } catch (PathException e) {
            // ok
        }
    }


    //--

    private void one(Object expected, Object obj, String path) {
        Path p;

        p = new Path(path);
        assertEquals(expected, p.selectOne(MODEL.instance(obj)).get());
    }

    private void all(Object obj, String path, Object ... expected) {
        Path p;
        List<Instance<?>> result;

        p = new Path(path);
        result = p.select(MODEL.instance(obj));
        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result.get(i).get());
        }
    }

    private Variable<?> access(Object obj, String path, boolean create) {
        return new Path(path).access(MODEL.instance(obj), create);
    }
}
