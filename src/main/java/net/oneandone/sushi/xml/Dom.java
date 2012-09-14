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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class Dom {
	/** @return element */
    public static Element addTextOpt(Element element, String text) {
        if ("".equals(text)) {
        	// nothing to do 
        } else {
        	Text txt = element.getOwnerDocument().createTextNode(text);
        	element.appendChild(txt);
        }
        return element;
    }
	
    // TODO: replace by iterator?
    public static List<Element> getAllChildElements(Element parent) {
        List<Element> result;
        NodeList nodes;
        int i;
        int max;
        Node node;

        result = new ArrayList<Element>();
        nodes = parent.getChildNodes();
        max = nodes.getLength();
        for (i = 0; i < max; i++) {
            node = nodes.item(i);
            if (node instanceof Element) {
                result.add((Element) node);
            }
        }
        return result;
    }

    public static Element getChildElementOpt(Element ele, String name) {
        List<Element> result;

        result = Dom.getChildElements(ele, name);
        switch (result.size()) {
        case 0:
            return null;
        case 1:
            return result.get(0);
        default:
            throw new DomException("too many elements: " + name);
        }
    }

    public static Element getChildElement(Element ele, String name) {
        List<Element> result;

        result = Dom.getChildElements(ele, name);
        switch (result.size()) {
        case 0:
            throw new DomException("missing element: " + name);
        case 1:
            return result.get(0);
        default:
            throw new DomException("too many elements: " + name);
        }
    }

    public static Element getFirstChildElement(Node parent, String local, Namespace namespace) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ((child instanceof Element) && Dom.matches(child, local, namespace)) {
                return (Element) child;
            }
        }
        return null;
    }

    /** Steps may be empty strings */
    public static List<Element> getChildElements(Element root, String ... steps) {
        List<Element> lst;
        
        lst = new ArrayList<Element>();
        doGetChildElements(root, steps, 0, lst);
        return lst;
    }

    private static void doGetChildElements(Element root, String[] steps, int i, List<Element> result) {
        if (i == steps.length) {
            result.add(root);
        } else {
            for (Element child : doGetChildElements(root, steps[i])) {
                doGetChildElements(child, steps, i + 1, result);
            }
        }
    }

    private static List<Element> doGetChildElements(Element parent, String name) {
        List<Element> result;
        NodeList nodes;
        int i;
        int max;
        Node node;
        Element element;

        result = new ArrayList<Element>();
        nodes = parent.getChildNodes();
        max = nodes.getLength();
        for (i = 0; i < max; i++) {
            node = nodes.item(i);
            if (node instanceof Element) {
                element = (Element) node;
                if (name.equals(element.getTagName())) {
                    result.add((Element) node);
                }
            }
        }
        return result;
    }

    //--
    
    public static String getString(Node node) {
        if (node instanceof Attr) {
            return ((Attr) node).getValue();
        } else if (node instanceof Element) {
            return Dom.getString((Element) node);
        } else {
            throw new RuntimeException(node.getClass().getName());
        }
    }

    public static String getString(Element root) {
        StringBuilder buffer;
        NodeList nodes;
        int i;
        int max;
        Node node;
        
        buffer = new StringBuilder();
        nodes = root.getChildNodes();
        max = nodes.getLength();
        for (i = 0; i < max; i++) {
            node = nodes.item(i);
            if (node instanceof Text) {
                buffer.append(node.getNodeValue());
            } else {
          		throw new IllegalArgumentException(node.getClass().getName());
            }
        }
        return buffer.toString();
    }
    
    //--
    
    public static String getAttribute(Element element, String name) {
        String attr;
        
        attr = getAttributeOpt(element, name);
        if (attr == null) {
            throw new DomException("missing attribute '" + name + "' in element '" + element.getTagName() + "'");
        }
        return attr;
    }

    public static String getAttribute(Element element, String name, String deflt) {
        String attr;

        attr = Dom.getAttributeOpt(element, name);
        return attr == null ? deflt : attr;
    }

    public static String getAttributeOpt(Element element, String name) {
        Attr attr;
        
        attr = element.getAttributeNode(name);
        if (attr == null) {
            return null;
        }
        return attr.getValue();
    }
    
    //--
    
    public static void require(Element ele, String expected) {
        String got;
        
        got = ele.getTagName();
        if (!expected.equals(ele.getTagName())) {
            throw new DomException("'" + expected + "' element expected, got '" + got + "'");
        }
    }
    
    public static void require(Node node, String local, Namespace namespace) {
        if (!Dom.matches(node, local, namespace)) {
        	throw new DomException("no such node in namespace " + namespace + ": " + local);
        }
    }

    //--
    
    public static boolean matches(Node node, String requiredLocalName, Namespace requiredNamespace) {
        return hasNamespace(node, requiredNamespace) && matchingLocalName(node, requiredLocalName);
    }

    private static boolean hasNamespace(Node node, Namespace requiredNamespace) {
        return requiredNamespace == null || requiredNamespace.hasUri(node.getNamespaceURI());
    }

    private static boolean matchingLocalName(Node node, String requiredLocalName) {
        if (requiredLocalName == null) {
            return true;
        } else {
            String localName = node.getLocalName();
            return requiredLocalName.equals(localName);
        }
    }


}
