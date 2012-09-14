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
package net.oneandone.sushi.metadata.model;

import net.oneandone.sushi.metadata.annotation.Sequence;
import net.oneandone.sushi.metadata.annotation.Type;
import net.oneandone.sushi.metadata.annotation.Value;

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
