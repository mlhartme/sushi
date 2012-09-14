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

import net.oneandone.sushi.metadata.annotation.AnnotationSchema;
import net.oneandone.sushi.metadata.model.Car;
import net.oneandone.sushi.metadata.model.Engine;
import net.oneandone.sushi.metadata.model.Kind;
import net.oneandone.sushi.metadata.model.Radio;
import net.oneandone.sushi.metadata.model.Vendor;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ComplexTypeTest {
    private static final Schema METADATA = new AnnotationSchema();

    @Test
    public void normal() {
        ComplexType type;
        Item item;
        Car demo;

        type = (ComplexType) METADATA.type(Car.class);
        assertEquals("car", type.getName());

        demo = (Car) type.newInstance();

        item = type.lookup("name");
        assertEquals("name", item.getName());
        assertEquals(String.class, item.getType().getType());
        assertEquals("", item.getOne(demo));
        item.setOne(demo, "foo");
        assertEquals("foo", item.getOne(demo));
        assertEquals(1, item.get(demo).size());

        item = type.lookup("seats");
        assertEquals("seats", item.getName());
        assertEquals(Integer.class, item.getType().getType());
        item.setOne(demo, 7);
        assertEquals(7, item.getOne(demo));
    }

    @Test
    public void sequence() {
        ComplexType type;
        Item item;
        Vendor demo;

        type = (ComplexType) METADATA.type(Vendor.class);
        assertEquals("vendor", type.getName());

        demo = (Vendor) type.newInstance();

        item = type.lookup("car");
        assertEquals("car", item.getName());
        assertEquals(METADATA.type(Car.class), item.getType());

        assertEquals(0, item.get(demo).size());
    }

    @Test
    public void clon() {
        Car orig;
        Car clone;

        orig = new Car("foo", Kind.SPORTS, 3, new Engine(true, 11), new Radio());
        clone = METADATA.instance(orig).clone().get();
        assertTrue(orig != clone);
        assertEquals(3, clone.getSeats());
        assertEquals("foo", clone.getName());
        assertEquals(true, clone.getEngine().getTurbo());
        assertEquals(11, clone.getEngine().getPs());
        assertNotNull(clone.getRadio());
    }
}

