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

package com.oneandone.sushi.metadata.model;

import com.oneandone.sushi.metadata.Schema;
import com.oneandone.sushi.metadata.annotation.AnnotationSchema;
import com.oneandone.sushi.metadata.reflect.ReflectSchema;
import org.junit.Before;

public abstract class ModelBase {
    public static final Schema MODEL = new AnnotationSchema();

    public static final Schema LISTMODEL = new ReflectSchema();

    protected Vendor vendor;
    protected Car audi;
    protected Car bmw;
    
    @Before
    public void setUp() {
        audi = new Car("audi", Kind.NORMAL, 4, new Engine(false, 90), new Radio());
        bmw = new Car("bmw", Kind.SPORTS, 2, new Engine(true, 200), null);
        vendor = new Vendor(audi, bmw);
    }
}
