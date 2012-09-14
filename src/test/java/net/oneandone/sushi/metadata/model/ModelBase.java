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

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.annotation.AnnotationSchema;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;
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
