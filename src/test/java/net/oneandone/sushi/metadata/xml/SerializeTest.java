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

import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.metadata.listmodel.All;
import net.oneandone.sushi.metadata.listmodel.Empty;
import net.oneandone.sushi.metadata.model.Car;
import net.oneandone.sushi.metadata.model.Engine;
import net.oneandone.sushi.metadata.model.Kind;
import net.oneandone.sushi.metadata.model.ModelBase;
import net.oneandone.sushi.metadata.model.Radio;
import net.oneandone.sushi.metadata.model.Vendor;
import net.oneandone.sushi.xml.Builder;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SerializeTest extends ModelBase {
    @Test
    public void string() {
        assertEquals(l("<string></string>"), run(""));
        assertEquals(l("<string>&lt;&gt;&amp;</string>"), run("<>&"));
        assertEquals(l("<string>ab</string>"), run("ab"));
    }
    
    @Test
    public void integer() {
        assertEquals(l("<int>9</int>"), run(9));
    }

    @Test
    public void enm() {
        assertEquals(l("<kind>van</kind>"), run(Kind.VAN));
    }
    
    @Test
    public void engine() {
        assertEquals(l("<engine>",
        		"  <turbo>true</turbo>",
        		"  <ps>1</ps>",
        		"</engine>"), run(new Engine(true, 1)));
    }

    @Test
    public void car() {
        assertEquals(l(
                "<car>", 
                "  <name></name>",
                "  <kind>normal</kind>",
                "  <seats>0</seats>",
                "  <engine>",
                "    <turbo>false</turbo>", 
                "    <ps>0</ps>",
                "  </engine>",
                "</car>"), run(new Car()));
    }

    @Test
    public void alias() {
        Vendor vendor;
        
        vendor = new Vendor();
        vendor.cars().add(new Car("foo", Kind.NORMAL, 5, new Engine(), new Radio()));
        vendor.cars().add(vendor.cars().get(0));
        assertEquals(l(
                "<vendor>",
                "  <id>0</id>",
                "  <car id='0'>",
                "    <name>foo</name>",
                "    <kind>normal</kind>", 
                "    <seats>5</seats>",
                "    <engine>",
                "      <turbo>false</turbo>",
                "      <ps>0</ps>",
                "    </engine>",
                "    <radio>",
                "      <cd>false</cd>",
                "      <speaker>0</speaker>",
                "    </radio>",
                "  </car>",
                "  <car idref='0'/>",
                "</vendor>") , run(vendor));
    }

    @Test
    public void carDom() throws IOException {
        org.w3c.dom.Element root;
        
        root = new Builder().createDocument("root").getDocumentElement();
        MODEL.instance(new Car()).toXml(root);
        assertEquals(l(
                "<root>",
                "  <car>",
                "    <name/>",
                "    <kind>normal</kind>",
                "    <seats>0</seats>",
                "    <engine>",
                "      <turbo>false</turbo>",
                "      <ps>0</ps>",
                "    </engine>",
                "  </car>",
                "</root>"),
                new net.oneandone.sushi.xml.Serializer().serialize(root));
    }
    
    private String run(Object obj) {
        return MODEL.instance(obj).toXml();
    }
    
    //-- tests with ListModel
    

    @Test
    public void empty() {
        assertEquals(l("<empty/>"), LISTMODEL.instance(new Empty()).toXml());
    }
    
    @Test
    public void list() {
        All all;
        
        all = new All();
        all.objects.add(new Empty());
        all.objects.add("");
        all.objects.add(2);
        assertEquals(l("<all>",
                "  <objects type='net.oneandone.sushi.metadata.listmodel.Empty'/>",
                "  <objects type='java.lang.String'></objects>",
                "  <objects type='java.lang.Integer'>2</objects>",
                "</all>"), 
                LISTMODEL.instance(all).toXml());
    }
    
    private static String l(String ... lines) {
        return OS.CURRENT.lines(lines);
    }
}
