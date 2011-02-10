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

package net.sf.beezle.sushi.metadata.store;

import net.sf.beezle.sushi.metadata.Instance;
import net.sf.beezle.sushi.metadata.model.Engine;
import net.sf.beezle.sushi.metadata.model.ModelBase;
import net.sf.beezle.sushi.metadata.model.Vendor;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class PropertyStoreTest extends ModelBase {
    @Test
    public void readEngine() {
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
    public void readPrefixedEngine() {
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
    public void vendor() {
        Instance<Vendor> i;
        Instance<Vendor> clone;
        Properties p;

        i = MODEL.instance(vendor);
        p = new Properties();
        i.toProperties(p, "foo");
        clone = MODEL.type(Vendor.class).loadProperties(p, "foo");
        assertEquals(2, clone.get().cars().size());
    }
}
