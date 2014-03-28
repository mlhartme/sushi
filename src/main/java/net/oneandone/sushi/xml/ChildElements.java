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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ChildElements implements Iterator<Element> {
    private final Namespace namespace;
    private final String localName;
    private Element next;

    /** all elements */
    public ChildElements(Element parent) {
        this(parent, null);
    }

    public ChildElements(Element parent, String localName) {
        this(parent, localName, null);
    }

    public ChildElements(Element parent, String localName, Namespace namespace) {
        NodeList lst;
        Node node;

        this.localName = localName;
        this.namespace = namespace;
        
        lst = parent.getChildNodes();
        for (int i = 0; i < lst.getLength(); i++) {
            node = lst.item(i);
            if ((node instanceof Element) && Dom.matches(node, localName, namespace)) {
                next = (Element) node;
                return;
            }
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public boolean hasNext() {
        return next != null;
    }

    public Element next() {
        Element result;
        Node node;

        if (next == null) {
            throw new NoSuchElementException();
        }
        result = next;
        node = next.getNextSibling();
        while (node != null) {
            if ((node instanceof Element) && Dom.matches(node, localName, namespace)) {
                next = (Element) node;
                return result;
            } else {
                node = node.getNextSibling();
            }
        }
        next = null;
        return result;
    }
}