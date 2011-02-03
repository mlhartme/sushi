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

package com.oneandone.sushi.metadata.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.metadata.Instance;
import com.oneandone.sushi.metadata.SimpleTypeException;
import com.oneandone.sushi.metadata.Type;
import com.oneandone.sushi.metadata.Variable;
import com.oneandone.sushi.metadata.listmodel.All;
import com.oneandone.sushi.metadata.listmodel.Empty;
import com.oneandone.sushi.metadata.model.Car;
import com.oneandone.sushi.metadata.model.Engine;
import com.oneandone.sushi.metadata.model.ModelBase;
import com.oneandone.sushi.metadata.model.Vendor;
import com.oneandone.sushi.metadata.reflect.ReflectSchema;
import org.xml.sax.SAXException;

public class LoaderTest extends ModelBase {
    // primitives
    
    @Test
    public void string() throws LoaderException {
        assertEquals("", str("<string></string>"));
        assertEquals("", str("<string/>"));
        assertEquals("a", str("<string>a</string>"));
        assertEquals("<>", str("<string>&lt;&gt;</string>"));
    }

    @Test
    public void integer() throws LoaderException {
        assertEquals(0, integer("<int>0</int>"));
        assertEquals(1, integer("<int>1</int>"));
        assertEquals(42, integer("<int>42</int>"));
        try {
            integer("<int></int>");
            fail();
        } catch (LoaderException e) {
            one(e, "''");
        }
        try {
            integer("<int>4a</int>");
            fail();
        } catch (LoaderException e) {
            one(e, "'4a'");
        }
    }

    @Test
    public void bool() throws LoaderException {
        assertEquals(true, bool("<boolean>true</boolean>"));
        assertEquals(false, bool("<boolean>false</boolean>"));
        try {
            bool("<boolean></boolean>");
            fail();
        } catch (LoaderException e) {
            one(e, "''");
        }
        try {
            bool("<boolean>yes</boolean>");
            fail();
        } catch (LoaderException e) {
            one(e, "'yes'");
        }
    }

    @Test
    public void lng() throws LoaderException {
        assertEquals((long) -2, loadXml(new ReflectSchema().type(Long.class), "<long>-2</long>").get());
    }

    //-- complex types
    
    @Test
    public void engine() throws LoaderException {
        Engine engine;
        
        engine = engine("<engine><turbo>true</turbo><ps>12</ps></engine>");
        assertEquals(true, engine.getTurbo());
        assertEquals(12, engine.getPs());
    }

    @Test
    public void vendor() throws LoaderException {
        Vendor vendor;
        Car car;
        
        vendor = (Vendor) loadXml(MODEL.type(Vendor.class), "<vendor>" +
        "  <id>100</id>" +
        "  <car><name>m3</name><kind>sports</kind><seats>2</seats>" +
        "    <engine><turbo>true</turbo><ps>200</ps></engine>" +
        "  </car>" +
        "  <car><name>golf</name><kind>normal</kind><seats>4</seats>" +
        "    <engine><turbo>false</turbo><ps>50</ps></engine>" +
        "    <radio><cd>true</cd><speaker>2</speaker></radio>" +
        "  </car>" +
        "</vendor>").get();
        assertEquals(100L, vendor.getId());
        assertEquals(2, vendor.cars().size());
        car = vendor.cars().get(0);
        assertEquals("m3", car.getName());
        assertNull(car.getRadio());
        car = vendor.cars().get(1);
        assertEquals("golf", car.getName());
        assertNotNull(car.getRadio());
        assertEquals(2, car.getRadio().getSpeaker());
    }

    @Test
    public void id() throws LoaderException {
        Vendor vendor;
        
        vendor = (Vendor) loadXml(MODEL.type(Vendor.class), "<vendor>" +
        "  <id>100</id>" +
        "  <car id='foo'><name>m3</name><kind>sports</kind><seats>2</seats>" +
        "    <engine><turbo>true</turbo><ps>200</ps></engine>" +
        "  </car>" +
        "  <car idref='foo'/>" +
        "</vendor>").get();
        assertEquals(100L, vendor.getId());
        assertEquals(2, vendor.cars().size());
        assertSame(vendor.cars().get(0), vendor.cars().get(1));
    }

    @Test(expected=LoaderException.class)
    public void idNotFound() throws LoaderException {
        Vendor vendor;
        
        vendor = (Vendor) loadXml(MODEL.type(Vendor.class), "<vendor>" +
        "  <id>100</id>" +
        "  <car idref='foo'/>" +
        "</vendor>").get();
        assertEquals(100L, vendor.getId());
        assertEquals(2, vendor.cars().size());
        assertSame(vendor.cars().get(0), vendor.cars().get(1));
    }
    
    @Test(expected=LoaderException.class)
    public void idDuplicate() throws LoaderException {
        Vendor vendor;
        
        vendor = (Vendor) loadXml(MODEL.type(Vendor.class), "<vendor>" +
        "  <id>100</id>" +
        "  <car id='foo'><name>m3</name><kind>sports</kind><seats>2</seats>" +
        "    <engine><turbo>true</turbo><ps>200</ps></engine>" +
        "  </car>" +
        "  <car id='foo'><name>m3</name><kind>sports</kind><seats>2</seats>" +
        "    <engine><turbo>true</turbo><ps>200</ps></engine>" +
        "  </car>" +
        "</vendor>").get();
        assertEquals(100L, vendor.getId());
        assertEquals(2, vendor.cars().size());
        assertSame(vendor.cars().get(0), vendor.cars().get(1));
    }

    @Test(expected=LoaderException.class)
    public void idUnexpectedContent() throws LoaderException {
        Vendor vendor;
        
        vendor = (Vendor) loadXml(MODEL.type(Vendor.class), "<vendor>" +
        "  <id>100</id>" +
        "  <car id='foo'><name>m3</name><kind>sports</kind><seats>2</seats>" +
        "    <engine><turbo>true</turbo><ps>200</ps></engine>" +
        "  </car>" +
        "  <car idref='foo'><name>m3</name><kind>sports</kind><seats>2</seats>" +
        "    <engine><turbo>true</turbo><ps>200</ps></engine>" +
        "  </car>" +
        "</vendor>").get();
        assertEquals(100L, vendor.getId());
        assertEquals(2, vendor.cars().size());
        assertSame(vendor.cars().get(0), vendor.cars().get(1));
    }
    //-- 
    
    @Test
    public void whitespaceBetweenElements() throws LoaderException {
        assertEquals(1, engine("<engine>\t\n<turbo>true</turbo> <ps>1</ps></engine>").getPs());
    }

    //--
    
    @Test
    public void malformed() throws LoaderException {
        try {
            str("<string>");
        } catch (LoaderException e) {
            one(e, "must start");
        }
    }

    @Test
    public void errorPosition() throws LoaderException {
        try {
            engine("<engine>\n<turbo>\n");
            fail();
        } catch (LoaderException e) {
            one(e, ":3:1");
        }
    }

    @Test
    public void mixedContent() {
        try {
            str("<string><ele/></string>");
            fail();
        } catch (LoaderException e) {
            oneLoader(e, "unknown element 'ele'");
        }
    }

    @Test
    public void contentBetweenElements() {
        try {
            engine("<engine><turbo>true</turbo>  abc  <ps>1</ps></engine>");
            fail();
        } catch (LoaderException e) {
            oneLoader(e, "unexpected content");
        }
    }
    
    @Test
    public void unknownField() {
        try {
            engine("<engine><turbo>true</turbo><ps>1</ps><unknown/></engine>");
            fail();
        } catch (LoaderException e) {
            oneLoader(e, "unknown element 'unknown'");
        }
    }
    
    @Test
    public void missingField() throws SimpleTypeException {
        Variable<Boolean> v;
        Engine engine;
        
        try {
            engine("<engine><ps>12</ps></engine>");
            fail();
        } catch (LoaderException e) {
            v = (Variable) oneVariable(e, "turbo").variable;
            assertEquals("turbo", v.item.getName());
            engine = (Engine) e.getLoaded();

            assertEquals(12, engine.getPs());
            assertEquals(false, engine.getTurbo());
            assertEquals(false, v.getOne());
            
            v.set(true);
            assertEquals(true, engine.getTurbo());
            assertEquals(true, v.getOne());
        }
    }

    //--
    
    private Object str(String str) throws LoaderException {
        return loadXml(new ReflectSchema().type(String.class), str).get();
    }

    private Object integer(String str) throws LoaderException {
        return loadXml(new ReflectSchema().type(Integer.class), str).get();
    }

    private Object bool(String str) throws LoaderException {
        return loadXml(new ReflectSchema().type(Boolean.class), str).get();
    }

    private Engine engine(String str) throws LoaderException {
        return (Engine) loadXml(MODEL.type(Engine.class), str).get();
    }
    
    //--
    
    private SAXLoaderException oneLoader(LoaderException e, String contains) {
        return (SAXLoaderException) one(e, contains);
    }

    private SAXVariableException oneVariable(LoaderException e, String contains) {
        return (SAXVariableException) one(e, contains);
    }
    
    private SAXException one(LoaderException e, String contains) {
        assertEquals(1, e.causes().size());
        assertTrue(e.getMessage(), e.getMessage().contains(contains));
        return e.causes().get(0);
    }

    private static final IO IO_OBJ = new IO();
    
    private static Instance<?> loadXml(Type type, String str) throws LoaderException {
        try {
            return type.loadXml(IO_OBJ.stringNode(str));
        } catch (LoaderException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    //--
    
    @Test
    public void object() throws Exception {
        Object obj;
        com.oneandone.sushi.fs.Node node;
        
        node = IO_OBJ.stringNode(
                "<object/>"
        );
        obj = LISTMODEL.type(Object.class).loadXml(node).get();
        assertTrue(obj instanceof Object);
    }
    
    @Test
    public void objectString() throws Exception {
        Object obj;
        com.oneandone.sushi.fs.Node node;
        
        node = IO_OBJ.stringNode(
                "<object type='java.lang.String'>foo</object>"
        );
        obj = LISTMODEL.type(Object.class).loadXml(node).get();
        assertEquals("foo", obj);
    }
    
    @Test
    public void list() throws Exception {
        All all;
        com.oneandone.sushi.fs.Node node;
        
        node = IO_OBJ.stringNode(
                "<all>" +
                "  <objects type='java.lang.Integer'>2</objects>" +
                "  <objects type='de.oneandone.sushi.metadata.listmodel.Empty'/>" +
                "  <objects type='java.lang.String'></objects>" +
                "</all>"                
        );
        all = (All) LISTMODEL.type(All.class).loadXml(node).get();
        assertEquals(3, all.objects.size());
        assertEquals(2, all.objects.get(0));
        assertTrue(all.objects.get(1) instanceof Empty);
        assertEquals("", all.objects.get(2));
    }
}
