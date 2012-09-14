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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
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
    private static final World WORLD = new World();

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

        file = WORLD.memoryNode(SCHEMA);
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
        new Builder(WORLD.memoryNode("xxx" + SCHEMA));
    }
}
