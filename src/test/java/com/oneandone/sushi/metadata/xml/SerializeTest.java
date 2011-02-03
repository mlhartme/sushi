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

package de.ui.sushi.metadata.xml;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import de.ui.sushi.io.OS;
import de.ui.sushi.metadata.listmodel.All;
import de.ui.sushi.metadata.listmodel.Empty;
import de.ui.sushi.metadata.model.Car;
import de.ui.sushi.metadata.model.Engine;
import de.ui.sushi.metadata.model.Kind;
import de.ui.sushi.metadata.model.ModelBase;
import de.ui.sushi.metadata.model.Radio;
import de.ui.sushi.metadata.model.Vendor;
import de.ui.sushi.xml.Builder;

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
    public void engine() throws IOException {
        assertEquals("<engine>\n  <turbo>true</turbo>\n  <ps>1</ps>\n</engine>\n", run(new Engine(true, 1)));
    }

    @Test
    public void car() throws IOException {
        assertEquals(
                "<car>\n  <name></name>\n  <kind>normal</kind>\n  <seats>0</seats>\n" +
                "  <engine>\n    <turbo>false</turbo>\n    <ps>0</ps>\n  </engine>\n" +
                "</car>\n", run(new Car()));
    }

    @Test
    public void alias() throws IOException {
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
                new de.ui.sushi.xml.Serializer().serialize(root));
    }
    
    private String run(Object obj) {
        return MODEL.instance(obj).toXml();
    }
    
    //-- tests with ListModel
    

    @Test
    public void empty() throws IOException {
        assertEquals("<empty/>" + LF, LISTMODEL.instance(new Empty()).toXml());
    }
    
    @Test
    public void list() throws IOException {
        All all;
        
        all = new All();
        all.objects.add(new Empty());
        all.objects.add("");
        all.objects.add(2);
        assertEquals("<all>" + LF +
                "  <objects type='de.oneandone.sushi.metadata.listmodel.Empty'/>" + LF +
                "  <objects type='java.lang.String'></objects>" + LF +
                "  <objects type='java.lang.Integer'>2</objects>" + LF + 
                "</all>" + LF, 
                LISTMODEL.instance(all).toXml());
    }
}
