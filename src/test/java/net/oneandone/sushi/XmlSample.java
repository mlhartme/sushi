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
package net.oneandone.sushi;

import net.oneandone.sushi.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XmlSample {
    private static final Xml XML = new Xml();
    
    public static void main(String[] args) throws SAXException {
        Document doc;
        
        doc = XML.getBuilder().parseString(
                "<foo>" +
                "  <bar a='1'/>" +
                "  <bar b='2'/>" +
                "</foo>");
        for (Node node : XML.getSelector().nodes(doc, "//bar")) {
            System.out.println(XML.getSerializer().serialize(node));
        }
    }
}
