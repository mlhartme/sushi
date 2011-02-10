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

package com.oneandone.sushi.metadata.xml;

import com.oneandone.sushi.metadata.ComplexType;
import com.oneandone.sushi.metadata.Item;
import com.oneandone.sushi.metadata.SimpleType;
import com.oneandone.sushi.metadata.Type;
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
