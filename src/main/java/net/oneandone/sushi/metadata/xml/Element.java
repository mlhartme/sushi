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

import net.oneandone.sushi.metadata.ComplexType;
import net.oneandone.sushi.metadata.Item;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.Type;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.List;
import java.util.Map;

public abstract class Element {
    public static Element create(Item<?> owner, Type type) {
        if (type instanceof SimpleType) {
            return new SimpleElement(owner, (SimpleType) type);
        } else {
            return new ComplexElement(owner, (ComplexType) type);
        }
    }
    
    //--

    /** null for root element */
    private final Item<?> owner;

    public String id;
    public String idref;
    
    protected Element(Item<?> owner) {
        this.owner = owner;
        this.id = null;
        this.idref = null;
    }
    
    public Item<?> getOwner() {
        return owner;
    }
    
    public abstract Item<?> lookup(String child);
    public abstract boolean isEmpty();
    public Object done(Map<String, Object> storage, List<SAXException> exceptions, Locator locator) {
        Object result;
        
        if (idref != null) {
            result = storage.get(idref);
            if (result == null) {
                exceptions.add(new SAXException("idref not found: " + idref));
            }
            if (!isEmpty()) {
                exceptions.add(new SAXException("unexpected content in idref element: " + idref));
            }
        } else {
            result = create(exceptions, locator);
        }
        if (id != null) {
            if (storage.put(id, result) != null) {
                exceptions.add(new SAXException("duplicate id: " + id));
            }
        }
        return result;
    }

    public abstract Object create(List<SAXException> exceptions, Locator locator);

    public abstract Type getType();
    
    public abstract void addChild(Item<?> item, Object child);
    public abstract boolean addContent(char[] ch, int start, int end);
}
