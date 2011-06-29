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

import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.io.OS;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class SerializerTest {
    private static final String LF = OS.CURRENT.lineSeparator.getSeparator();

    private static final Builder BUILDER = new Builder();
    private static final Selector SELECTOR = new Selector();
    private static final Serializer SERIALIZER = new Serializer();

    @Test
    public void escape() throws SAXException {
        Document doc;
        String str;

        assertEquals("", Serializer.escapeEntities("", true));
        assertEquals(" \t\r\n", Serializer.escapeEntities(" \t\r\n", true));
        assertEquals("abc", Serializer.escapeEntities("abc", true));
        assertEquals("&lt;", Serializer.escapeEntities("<", true));
        assertEquals("abc&lt;&gt;&amp;&apos;&quot;xyz", Serializer.escapeEntities("abc<>&'\"xyz", true));
        for (char c = 1; c < 128; c++) {
            str = "<doc attr='"
                + Serializer.escapeEntities("" + c, false) + "'>"
                + Serializer.escapeEntities("" + c, false) + "</doc>";
            doc = BUILDER.parseString(str);
            isChar(c, Dom.getString(SELECTOR.node(doc, "/doc")));
            isChar(c, Dom.getString(SELECTOR.node(doc, "/doc/@attr")));
        }
    }

    private void isChar(char c, String str) {
        if (str.equals(Character.toString(c))) {
            return;
        }
        if (Character.isWhitespace(c) && str.trim().isEmpty()) {
            return;
        }
        if (str.contains("illegal character") && str.contains(Integer.toString(c))) {
            return;
        }
        fail("expected code " + (int) c + ", got: " + str);
    }

    @Test
    public void serialize() throws Exception {
        checkSerialize("<root/>" + LF, "<root/>", "/");
        checkSerialize("<root>" + LF + "  <a/>" + LF + "</root>" + LF, "<root><a/></root>", "/");
        checkSerialize("<root/>" + LF, "<root/>", "/root");
        checkSerialize("<a/>" + LF, "<root><a/></root>", "/root/a");
        checkSerialize("mhm", "<root>mhm</root>", "/root/text()");
        checkSerialize("<root>white space</root>" + LF, "<root>white space</root>", "/");
        checkSerialize("<root>white  space</root>" + LF, "<root>white  space</root>", "/");
        checkSerialize("<root>white space </root>" + LF, "<root>white space </root>", "/");
    }

    @Test
    public void exceptn() throws Exception {
        OutputStream stream;
        final IOException e;

        e = new IOException();
        stream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw e;
            }

        };
        try {
            SERIALIZER.serialize(new DOMSource(BUILDER.parseString("<foo/>")), new StreamResult(stream));
            fail();
        } catch (IOException ex) {
            assertSame(e, ex);
        }
    }
    @Test
    public void serializeWithEncoding() throws Exception {
        Document doc;
        FileNode file;

        doc = BUILDER.parseString("<a><b/></a>");
        file = new World().getTemp().createTempFile();
        SERIALIZER.serialize(doc, file);
        assertEquals(OS.CURRENT.lines("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<a>", "  <b/>", "</a>"),
        		file.readString());
    }

    @Test
    public void serializeChildren() throws Exception {
        checkSerializeChildren("", "<root/>", "/root");
        checkSerializeChildren("<a/>", "<root><a/></root>", "/root");
        checkSerializeChildren("<p>1</p>" + LF +"  <p>2</p>", "<root><a><p>1</p><p>2</p></a></root>", "/root/a");
        checkSerializeChildren("<p>1<inner/>2</p>", "<root><p>1<inner/>2</p></root>", "/root");
        checkSerializeChildren("<p>1<inner>I</inner>2</p>", "<root><p>1<inner>I</inner>2</p></root>", "/root");
    }

    @Test
    public void serializeDocChildren() throws Exception {
        checkSerializeDocChildren("", "<root/>");
        checkSerializeDocChildren("<a/>", "<root><a/></root>");
        checkSerializeDocChildren("text", "<root>text</root>");
        checkSerializeDocChildren("abc<x>text</x>ABC", "<root>abc<x>text</x>ABC</root>");
    }

    //--

    private void checkSerialize(String expected, String doc, String path) throws Exception {
        Node node = SELECTOR.node(BUILDER.parseString(doc), path);
        assertEquals(expected, SERIALIZER.serialize(node));
    }

    private void checkSerializeChildren(String expected, String doc, String path) throws Exception {
        Element element = SELECTOR.element(BUILDER.parseString(doc), path);
        assertEquals(expected, SERIALIZER.serializeChildren(element));
    }

    private void checkSerializeDocChildren(String expected, String doc) throws Exception {
        assertEquals(expected, SERIALIZER.serializeChildren(BUILDER.parseString(doc)));
    }
}
