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

package net.sf.beezle.sushi.metadata.xml;

import net.sf.beezle.sushi.metadata.*;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexElement extends Element {
    private final ComplexType type;
    private final Map<Item<?>, List<Object>> children;

    public ComplexElement(Item<?> owner, ComplexType type) {
        super(owner);
        
        if (owner != null && !owner.getType().getType().isAssignableFrom(type.getType())) {
            throw new IllegalArgumentException();
        }
        this.type = type;
        this.children = new HashMap<Item<?>, List<Object>>();
        for (Item<?> item : type.items()) {
            children.put(item, new ArrayList<Object>());
        }
    }

    @Override
    public Type getType() {
        return type;
    }
    
    @Override
    public void addChild(Item<?> item, Object obj) {
        children.get(item).add(obj);
    }
    
    @Override
    public boolean isEmpty() {
        for (List<Object> values : children.values()) {
            if (values.size() > 0) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Object create(List<SAXException> exceptions, Locator locator) {
        Object object;
        Item item;
        
        object = type.newInstance();
        for (Map.Entry<Item<?>, List<Object>> entry : children.entrySet()) {
            item = entry.getKey();
            try {
                item.set(object, entry.getValue());
            } catch (ItemException e) {
                exceptions.add(new SAXVariableException(new Variable<Object>(object, item), locator, e));
            }            
        }
        return object;
    }

    @Override
    public boolean addContent(char[] ch, int ofs, int len) {
        int max;
        
        max = ofs + len;
        for (int i = ofs; i < max; i++) {
            if (!Character.isWhitespace(ch[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Item lookup(String child) {
        return type.lookupXml(child);
    }
}
