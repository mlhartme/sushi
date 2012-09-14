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

import net.oneandone.sushi.metadata.model.Kind;
import net.oneandone.sushi.metadata.model.ModelBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SimpleTypeTest extends ModelBase {
    @Test
    public void string() {
        SimpleType type; 
        
        type = (SimpleType) MODEL.type(String.class);
        assertEquals("a", type.valueToString("a"));
    }

    @Test
    public void integer() throws SimpleTypeException {
        SimpleType type; 
        
        type = (SimpleType) MODEL.type(Integer.TYPE);
        assertEquals("2", type.valueToString(2));
        assertEquals(2, type.stringToValue("2"));
    }

    @Test
    public void longX() throws SimpleTypeException {
        check(Long.TYPE, "2", new Long(2));
    }
    
    @Test
    public void floatX() throws SimpleTypeException {
        check(Float.class, "2", new Float(2), "2.0");
        check(Float.TYPE, "2.0", new Float(2));
    }

    @Test
    public void doubleX() throws SimpleTypeException {
        check(Double.TYPE, "4.9", new Double(4.9), "4.9");
        check(Double.class, "2.0", new Double(2));
    }
    
    @Test
    public void booleanX() throws SimpleTypeException {
        SimpleType type; 
        
        type = (SimpleType) MODEL.type(Boolean.TYPE);
        assertEquals("true", type.valueToString(true));
        assertEquals("false", type.valueToString(false));
        assertEquals(true, type.stringToValue("true"));
        assertEquals(false, type.stringToValue("false"));
    }
    
    @Test
    public void integerObject() throws SimpleTypeException {
        SimpleType type; 
        
        type = (SimpleType) MODEL.type(Integer.class);
        assertEquals("22", type.valueToString(22));
        assertEquals(22, type.stringToValue("22"));
        try {
            type.stringToValue("22a");
            fail();
        } catch (SimpleTypeException e) {
            // ok;
        }
    }

    @Test
    public void enm() throws SimpleTypeException {
        SimpleType type; 
        
        type = (SimpleType) MODEL.type(Kind.class);
        assertEquals("van", type.valueToString(Kind.VAN));
        assertEquals(Kind.PICKUP, type.stringToValue("pickup"));
        try {
            type.stringToValue("nosuchcar");
            fail();
        } catch (SimpleTypeException e) {
            // ok
        }
    }
    
    private void check(Class<?> clazz, String str, Object value) throws SimpleTypeException {
        check(clazz, str, value, str);
    }
    
    private void check(Class<?> clazz, String str, Object value, String out) throws SimpleTypeException {
        SimpleType type;
        
        type = (SimpleType) MODEL.type(clazz);
        assertEquals(value, type.stringToValue(str));
        assertEquals(out, type.valueToString(value));
    }
}
