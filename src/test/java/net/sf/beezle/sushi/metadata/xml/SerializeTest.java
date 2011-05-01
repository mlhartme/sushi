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

package net.sf.beezle.sushi.metadata.xml;

import net.sf.beezle.sushi.io.OS;
import net.sf.beezle.sushi.metadata.listmodel.All;
import net.sf.beezle.sushi.metadata.listmodel.Empty;
import net.sf.beezle.sushi.metadata.model.*;
import net.sf.beezle.sushi.xml.Builder;
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
                "<car>",
                "<name/>",
                "<kind>normal</kind>",
                "<seats>0</seats>",
                "<engine>",
                "<turbo>false</turbo>",
                "<ps>0</ps>",
                "</engine>",
                "</car>",
                "</root>"),
                new net.sf.beezle.sushi.xml.Serializer().serialize(root));
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
                "  <objects type='net.sf.beezle.sushi.metadata.listmodel.Empty'/>",
                "  <objects type='java.lang.String'></objects>",
                "  <objects type='java.lang.Integer'>2</objects>",
                "</all>"), 
                LISTMODEL.instance(all).toXml());
    }
    
    private static String l(String ... lines) {
        return OS.CURRENT.lines(lines);
    }
}
