/**
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
package net.sf.beezle.sushi.metadata;

import net.sf.beezle.sushi.metadata.annotation.AnnotationSchema;
import net.sf.beezle.sushi.metadata.model.Car;
import net.sf.beezle.sushi.metadata.model.Engine;
import net.sf.beezle.sushi.metadata.model.Kind;
import net.sf.beezle.sushi.metadata.model.Radio;
import net.sf.beezle.sushi.metadata.model.Vendor;
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

