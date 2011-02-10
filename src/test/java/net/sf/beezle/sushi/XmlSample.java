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

package com.oneandone.sushi;

import com.oneandone.sushi.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XmlSample {
    private static final Xml XML = new Xml();
    
    public static void main(String[] args) throws SAXException {
        Document doc;
        
        doc = XML.builder.parseString(
                "<foo>" +
                "  <bar a='1'/>" +
                "  <bar b='2'/>" +
                "</foo>");
        for (Node node : XML.selector.nodes(doc, "//bar")) {
            System.out.println(XML.serializer.serialize(node));
        }
    }
}
