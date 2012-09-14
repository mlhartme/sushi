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
package net.oneandone.sushi.xml;

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
