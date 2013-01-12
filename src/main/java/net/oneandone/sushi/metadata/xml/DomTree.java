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
package net.oneandone.sushi.metadata.xml;

import net.oneandone.sushi.xml.Builder;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** You'll normally not use this class directly, use Instance.toXml instead */
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
