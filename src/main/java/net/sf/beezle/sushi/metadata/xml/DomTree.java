/**
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
package net.sf.beezle.sushi.metadata.xml;

import net.sf.beezle.sushi.xml.Builder;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** You'll normally not use this class directly, use Data.toXml instead */
public class DomTree extends Tree {
    private final List<Element> parents;
    
    public DomTree(Element root) {
        parents = new ArrayList<Element>();
        parents.add(root);
    }

    @Override
    public Element done() {
        if (parents.size() != 1) {
            throw new IllegalStateException("" + parents.size());
        }
        return parents.get(0);
    }

    @Override
    public void ref(String name, int idref) throws IOException {
        Element element;
        
        element = Builder.element(parent(), name);
        element.setAttribute("idref", Integer.toString(idref));
        parents.add(element);
    }

    @Override
    public void begin(String name, int id, String type, boolean withEnd) throws IOException {
        Element element;
        
        element = Builder.element(parent(), name);
        if (id != -1) {
            element.setAttribute("id", Integer.toString(id));
        }
        type(element, type);
        parents.add(element);
    }

    @Override
    public void end(String name) {
        parents.remove(parents.size() - 1);
    }

    @Override
    public void text(String name, String type, String content) {
        Element element;
        
        element = (Element) Builder.textElement(parent(), name, content).getParentNode();
        type(element, type);
    }

    private void type(Element element, String type) {
        if (type != null) {
            element.setAttribute("type", type);
        }
    }
    
    private Element parent() {
        return parents.get(parents.size() - 1);
    }
}
