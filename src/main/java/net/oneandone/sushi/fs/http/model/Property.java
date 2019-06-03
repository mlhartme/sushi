/*
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
package net.oneandone.sushi.fs.http.model;

import net.oneandone.sushi.xml.Dom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * For MultiStatus
 */
public class Property {
    public static Property fromXml(Element propertyElement) {
        Name name;
        Object value;
        List<?> content;

        name = Name.fromXml(propertyElement);
        content = getChildElementsOrTexts(propertyElement);
        switch (content.size()) {
            case 0:
                value = null;
                break;
            case 1:
                Node n = (Node) content.get(0);
                if (n instanceof Element) {
                    value = n;
                } else {
                    value = n.getNodeValue();
                }
                break;
            default:
                value = content;
                break;
        }
        return new Property(name, value);
    }

    private static List<Node> getChildElementsOrTexts(Node parent) {
        List<Node> content = new ArrayList<>();
        Node child;

        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            child = children.item(i);
            if ((child instanceof Element) || (child instanceof Text)) {
                content.add(child);
            }
        }
        return content;
    }

    private final Name name;
    private final Object value;

    public Property(Name name, Object value) {
        this.name = name;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        int hashCode = getName().hashCode();
        if (getValue() != null) {
            hashCode += getValue().hashCode();
        }
        return hashCode % Integer.MAX_VALUE;
    }

    @Override
    public boolean equals(Object obj) {
        Property p;

        if (obj instanceof Property) {
            p = (Property) obj;
            return name.equals(p.name) && Objects.equals(value, p.value);
        }
        return false;
    }

    public Element addXml(Element parent) {
        Document document;
        Element elem;
        Object obj;
        Node n;

        document = parent.getOwnerDocument();
        elem = getName().addXml(parent);
        obj = getValue();
        if (obj != null) {
            if (obj instanceof Node) {
                n = document.importNode((Node) obj, true);
                elem.appendChild(n);
            } else if (obj instanceof Node[]) {
                for (int i = 0; i < ((Node[]) obj).length; i++) {
                    n = document.importNode(((Node[]) obj)[i], true);
                    elem.appendChild(n);
                }
            } else if (obj instanceof Collection<?>) {
                for (Object entry : (Collection<?>) obj) {
                    if (entry instanceof Node) {
                        n = document.importNode((Node) entry, true);
                        elem.appendChild(n);
                    } else {
                        Dom.addTextOpt(elem, entry.toString());
                    }
                }
            } else {
                Dom.addTextOpt(elem, obj.toString());
            }
        }
        return elem;
    }

    public Name getName() {
        return name;
    }
}
