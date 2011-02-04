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

import com.oneandone.sushi.util.Strings;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Selector {
    private final XPathFactory factory;
    private final Map<String, String[]> simples;
    private final Map<String, XPathExpression> normals;
    
    public Selector() {
        this.factory = XPathFactory.newInstance();
        this.simples = new HashMap<String, String[]>();
        this.normals = new HashMap<String, XPathExpression>();
    }

    //--
    
    public List<Node> nodes(Node context, String path) {
        if (context instanceof Element && isSimple(path)) {
            return (List) nodesSimple((Element) context, path);
        } else {
            return nodesNormal(context, path);
        }
    }

    public Node node(Node context, String xpath) {
        List<Node> result;

        result = nodes(context, xpath);
        if (result.size() == 0) {
            return null;
        } else {
            return result.get(0);
        }
    }

    public List<Element> elements(Node root, String path) {
        return (List) nodes(root, path);
    }
    
    public Element element(Node root, String path) throws XmlException {
        Element element;
        
        element = elementOpt(root, path);
        if (element == null) {
            throw new XmlException("element not found: " + path);
        }
        return element;
    }

    public Element elementOpt(Node root, String path) throws XmlException {
        Node node;

        node = node(root, path);
        if (node == null) {
            return null;
        } else if (node instanceof Element) {
            return (Element) node;
        } else {
            throw new XmlException("element expected: " + path);
        }
    }

    public int integer(Node element, String path) throws XmlException {
        String str;
        
        str = string(element, path);
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new XmlException("number expected node " + path + ": " + str);
        }
    }
    
    /** Checkstyle rejects method name long_  */
    public long longer(Node element, String path) throws XmlException {
        String str;
        
        str = string(element, path);
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new XmlException("number expected node " + path + ": " + str);
        }
    }

    public String string(Node ele, String path) throws XmlException {
        String str;
        
        str = stringOpt(ele, path);
        if (str == null) {
            throw new XmlException("no such node: " + path);            
        }
        return str;
    }
    
    public String stringOpt(Node ele, String path) throws XmlException {
        List<Node> lst;
        
        lst = nodes(ele, path);
        switch (lst.size()) {
        case 0:
            return null;
        case 1:
            return Dom.getString(lst.get(0));
        default:
            throw new XmlException("node ambiguous: " + path);
        }
    }
    
    //--

    public List<Element> nodesSimple(Element context, String path) {
        String[] steps;
        
        steps = simples.get(path);
        if (steps == null) {
            steps = Strings.toArray(Strings.split("/", path));
            simples.put(path, steps);
        }
        return Dom.getChildElements(context, steps);
    }

    public List<Node> nodesNormal(Node context, String expression) {
        int size;
        NodeList nodes;
        List<Node> result;
        
        try {
            nodes = (NodeList) compile(expression).evaluate(context, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException("invalid xpath: " + expression, e);
        }
        size = nodes.getLength();
        result = new ArrayList<Node>(size);
        for (int i = 0; i < size; i++) {
            result.add(nodes.item(i));
        }
        return result;
    }

    //--
    
    public static boolean isSimple(String path) {
        int i;
        int max;
        char c;
        
        max = path.length();
        if (max == 0) {
            return false;
        }
        if (path.charAt(0) == '/') {
            return false;
        }
        for (i = 0; i < max; i++) {
            c = path.charAt(i);
            if (c == '/') {
                if (i > 0 && path.charAt(i - 1) == '/') {
                    return false;
                }
            } else {
                if (c != '_' && c != '-' && !Character.isLetter(c) && !Character.isDigit(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    private XPathExpression compile(String path) {
        XPathExpression cached;
        XPath xpath;
        
        cached = normals.get(path);
        if (cached == null) {
            xpath = factory.newXPath();
            try {
                cached = xpath.compile(path);
            } catch (XPathExpressionException e) {
                throw new RuntimeException("invalid xpath: " + path, e);
            }
            normals.put(path, cached);
        }
        return cached;
    }

}
