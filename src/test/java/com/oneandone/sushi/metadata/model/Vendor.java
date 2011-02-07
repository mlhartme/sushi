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

import com.oneandone.sushi.metadata.annotation.Sequence;
import com.oneandone.sushi.metadata.annotation.Type;
import com.oneandone.sushi.metadata.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Type
public class Vendor {
    @Value private long id;
    @Sequence(Car.class) private List<Car> cars;
    
    public Vendor() {
        this(new Car[] {});
    }
    
    public Vendor(Car ... cars) {
        this.cars = new ArrayList<Car>(Arrays.asList(cars));
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }
    
    public List<Car> cars() {
        return cars;
    }
}
