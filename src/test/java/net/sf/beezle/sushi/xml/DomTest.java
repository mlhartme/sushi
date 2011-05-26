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

package net.sf.beezle.sushi.xml;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class DomTest {
    private static final Builder BUILDER = new Builder();

    @Test
    public void getElements() {
        Document doc;
        List<Element> lst;

        doc = BUILDER.literal("<root><a><b/><b/></a><A><b/></A></root>");
        lst = Dom.getChildElements(doc.getDocumentElement());
        assertEquals(1, lst.size());
        assertSame(doc.getDocumentElement(), lst.get(0));

        lst = Dom.getChildElements(doc.getDocumentElement(), "a");
        assertEquals(1, lst.size());
        assertSame("a", lst.get(0).getTagName());

        lst = Dom.getAllChildElements(doc.getDocumentElement());
        assertEquals(2, lst.size());
        assertSame("a", lst.get(0).getTagName());
        assertSame("A", lst.get(1).getTagName());

        lst = Dom.getChildElements(doc.getDocumentElement(), "a", "b");
        assertEquals(2, lst.size());
        assertSame("b", lst.get(0).getTagName());
        assertSame("b", lst.get(1).getTagName());
    }

    //-- getText

    @Test
    public void empty() {
        assertEquals("", getString("<empty></empty>"));
        assertEquals("", getString("<empty/>"));
    }

    @Test
    public void normal() {
        assertEquals("tanjev", getString("<doc>tanjev</doc>"));
    }

    @Test
    public void whitespace() {
        assertEquals("\n tanjev ", getString("<doc>\n tanjev </doc>"));
    }

    @Test
    public void noElement() {
        try {
            getString("<doc><notanelement/></doc>");
            fail();
        } catch (IllegalArgumentException e) {
            //ok
        }
    }

    @Test
    public void mixContent() {
        try {
            getString("<doc>foo<notanelement/>bar</doc>");
            fail();
        } catch (IllegalArgumentException e) {
            //ok
        }
    }

    private String getString(String docString) {
        Document doc;

        try {
            doc = BUILDER.parseString(docString);
        } catch (SAXException e) {
            throw new RuntimeException(docString, e);
        }
        return Dom.getString(doc.getDocumentElement());
    }

}
