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
package net.oneandone.sushi.metadata.properties;

import net.oneandone.sushi.metadata.Instance;
import net.oneandone.sushi.metadata.model.Engine;
import net.oneandone.sushi.metadata.model.ModelBase;
import net.oneandone.sushi.metadata.model.Vendor;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class IOTest extends ModelBase {
    @Test
    public void external() {
        check("", "");
        check("java.home", "javaHome");
        check(".f.o.o", "FOO");
    }

    private void check(String external, String internal) {
        assertEquals(external, IO.toExternal(internal));
        assertEquals(internal, IO.fromExternal(external));
    }

    @Test
    public void readEngine() throws LoadException {
        Properties p;
        Engine engine;
        
        p = new Properties();
        p.put("", Engine.class.getName());
        p.put("turbo", "true");
        p.put("ps", "12");
        engine = MODEL.type(Engine.class).<Engine>loadProperties(p).get();
        assertEquals(12, engine.getPs());
        assertEquals(true, engine.getTurbo());
    }
    
    @Test
    public void readPrefixedEngine() throws LoadException {
        Properties p;
        Engine engine;
        
        p = new Properties();
        p.put("foo", Engine.class.getName());
        p.put("foo/turbo", "true");
        p.put("foo/ps", "12");
        engine = MODEL.type(Engine.class).<Engine>loadProperties(p, "foo").get();
        assertEquals(12, engine.getPs());
        assertEquals(true, engine.getTurbo());
    }
    
    @Test
    public void vendor() throws LoadException {
        Instance<Vendor> i;
        Instance<Vendor> clone;
        Properties p;

        i = MODEL.instance(vendor);
        p = new Properties();
        i.toMap(p, "foo");
        clone = MODEL.type(Vendor.class).loadProperties(p, "foo");
        assertEquals(2, clone.get().cars().size());
    }
}
