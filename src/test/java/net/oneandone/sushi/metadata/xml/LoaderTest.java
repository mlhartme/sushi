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
package net.oneandone.sushi.metadata.xml;

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.Instance;
import net.oneandone.sushi.metadata.Type;
import net.oneandone.sushi.metadata.Variable;
import net.oneandone.sushi.metadata.listmodel.All;
import net.oneandone.sushi.metadata.listmodel.Empty;
import net.oneandone.sushi.metadata.model.Car;
import net.oneandone.sushi.metadata.model.Engine;
import net.oneandone.sushi.metadata.model.ModelBase;
import net.oneandone.sushi.metadata.model.Vendor;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test(expected=LoaderException.class)
    public void malformed() throws LoaderException {
        str("<string>");
    }

    @Test
    public void errorPosition() {
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
    public void missingField() {
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

    private static final World WORLD = World.createMinimal();

    private static Instance<?> loadXml(Type type, String str) throws LoaderException {
        try {
            return type.loadXml(WORLD.memoryNode(str));
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
        net.oneandone.sushi.fs.Node node;

        node = WORLD.memoryNode("<object/>");
        obj = LISTMODEL.type(Object.class).loadXml(node).get();
        assertNotNull(obj);
    }

    @Test
    public void objectString() throws Exception {
        Object obj;
        net.oneandone.sushi.fs.Node node;

        node = WORLD.memoryNode("<object type='java.lang.String'>foo</object>");
        obj = LISTMODEL.type(Object.class).loadXml(node).get();
        assertEquals("foo", obj);
    }

    @Test
    public void list() throws Exception {
        All all;
        net.oneandone.sushi.fs.Node node;

        node = WORLD.memoryNode(
                "<all>" +
                "  <objects type='java.lang.Integer'>2</objects>" +
                "  <objects type='net.oneandone.sushi.metadata.listmodel.Empty'/>" +
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
