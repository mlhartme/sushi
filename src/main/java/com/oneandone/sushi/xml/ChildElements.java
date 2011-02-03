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

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

    /**
     * @throws UnsupportedOperationException
     */
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