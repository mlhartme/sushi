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

package com.oneandone.sushi.xml;

import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.fs.Node;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BuilderTest {
    private static final IO IO_OBJ = new IO();
    
    private Builder builder;

    @Test
    public void reader() throws Exception {
        StringReader src;
        Document doc;
        final List<String> closed;

        closed = new ArrayList<String>();
        src = new StringReader("<xml/>") {
            @Override
            public void close() {
                closed.add("str");
                super.close();
            }
        };
        builder = new Builder();
        doc = builder.parse(src);
        assertEquals("xml", doc.getDocumentElement().getNodeName());
        assertEquals(1, closed.size());
    }

    @Test
    public void inputStream() throws Exception {
        ByteArrayInputStream src;
        Document doc;
        final List<String> closed;

        closed = new ArrayList<String>();
        src = new ByteArrayInputStream("<xml/>".getBytes()) {
            @Override
            public void close() throws IOException {
                closed.add("str");
                super.close();
            }
        };
        builder = new Builder();
        doc = builder.parse(src);
        assertEquals("xml", doc.getDocumentElement().getNodeName());
        assertEquals(1, closed.size());
    }

    //--

    private static final String SCHEMA =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>\n" +
        "  <xs:element name='a'>\n" +
        "    <xs:complexType>\n" +
        "      <xs:sequence>\n" +
        "        <xs:element name='num' type='xs:int'/>\n" +
        "        <xs:element name='string' type='xs:string' minOccurs='0' maxOccurs='unbounded'/>\n" +
        "      </xs:sequence>\n" +
        "    </xs:complexType>\n"+
        "  </xs:element>\n" +
        "</xs:schema>";

    @Test
    public void validate() throws IOException, SAXException {
        Node file;
        Builder builder;

        file = IO_OBJ.stringNode(SCHEMA);
        builder = new Builder(file);
        builder.parseString("<a><num>1</num><string/><string/></a>");
        try {
            builder.parseString("<a><num></num></a>");
            fail();
        } catch (SAXParseException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("cvc-datatype-valid.1.2.1"));
        }
        try {
            builder.parseString("<a></a>");
            fail();
        } catch (SAXParseException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("cvc-complex-type.2.4.b"));
        }
    }

    @Test(expected=SAXParseException.class)
    public void invalidSchema() throws IOException, SAXException {
        new Builder(IO_OBJ.stringNode("xxx" + SCHEMA));
    }
}
