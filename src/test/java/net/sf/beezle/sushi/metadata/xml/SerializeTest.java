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
import net.sf.beezle.sushi.metadata.model.Car;
import net.sf.beezle.sushi.metadata.model.Engine;
import net.sf.beezle.sushi.metadata.model.Kind;
import net.sf.beezle.sushi.metadata.model.ModelBase;
import net.sf.beezle.sushi.metadata.model.Radio;
import net.sf.beezle.sushi.metadata.model.Vendor;
import net.sf.beezle.sushi.xml.Builder;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class SerializeTest extends ModelBase {
    @Test
    public void string() {
        assertEquals("<string></string>\n", run(""));
        assertEquals("<string>&lt;&gt;&amp;</string>\n", run("<>&"));
        assertEquals("<string>ab</string>\n", run("ab"));
    }
    
    @Test
    public void integer() {
        assertEquals("<int>9</int>\n", run(9));
    }

    @Test
    public void enm() {
        assertEquals("<kind>van</kind>\n", run(Kind.VAN));
    }
    
    @Test
    public void engine() {
        assertEquals("<engine>\n  <turbo>true</turbo>\n  <ps>1</ps>\n</engine>\n", run(new Engine(true, 1)));
    }

    @Test
    public void car() {
        assertEquals(
                "<car>\n  <name></name>\n  <kind>normal</kind>\n  <seats>0</seats>\n" +
                "  <engine>\n    <turbo>false</turbo>\n    <ps>0</ps>\n  </engine>\n" +
                "</car>\n", run(new Car()));
    }

    @Test
    public void alias() {
        Vendor vendor;
        
        vendor = new Vendor();
        vendor.cars().add(new Car("foo", Kind.NORMAL, 5, new Engine(), new Radio()));
        vendor.cars().add(vendor.cars().get(0));
        assertEquals(
                "<vendor>\n" +
                "  <id>0</id>\n" +
                "  <car id='0'>\n" +
                "    <name>foo</name>\n" +
                "    <kind>normal</kind>\n" + 
                "    <seats>5</seats>\n" + 
                "    <engine>\n" + 
                "      <turbo>false</turbo>\n" + 
                "      <ps>0</ps>\n" + 
                "    </engine>\n" + 
                "    <radio>\n" + 
                "      <cd>false</cd>\n" + 
                "      <speaker>0</speaker>\n" +
                "    </radio>\n" + 
                "  </car>\n" + 
                "  <car idref='0'/>\n" + 
                "</vendor>\n" , run(vendor));
    }

    private static final String LF = OS.CURRENT.lineSeparator;
    
    @Test
    public void carDom() throws IOException {
        org.w3c.dom.Element root;
        
        root = new Builder().createDocument("root").getDocumentElement();
        MODEL.instance(new Car()).toXml(root);
        assertEquals(
                "<root>" + LF +
                "<car>" + LF +
                "<name/>" + LF +
                "<kind>normal</kind>" + LF +
                "<seats>0</seats>" + LF +
                "<engine>" + LF +
                "<turbo>false</turbo>" + LF +
                "<ps>0</ps>" + LF +
                "</engine>" + LF +
                "</car>" + LF +
                "</root>" + LF, 
                new net.sf.beezle.sushi.xml.Serializer().serialize(root));
    }
    
    private String run(Object obj) {
        return MODEL.instance(obj).toXml();
    }
    
    //-- tests with ListModel
    

    @Test
    public void empty() {
        assertEquals("<empty/>" + LF, LISTMODEL.instance(new Empty()).toXml());
    }
    
    @Test
    public void list() {
        All all;
        
        all = new All();
        all.objects.add(new Empty());
        all.objects.add("");
        all.objects.add(2);
        assertEquals("<all>" + LF +
                "  <objects type='net.sf.beezle.sushi.metadata.listmodel.Empty'/>" + LF +
                "  <objects type='java.lang.String'></objects>" + LF +
                "  <objects type='java.lang.Integer'>2</objects>" + LF + 
                "</all>" + LF, 
                LISTMODEL.instance(all).toXml());
    }
}
