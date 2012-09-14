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
import net.oneandone.sushi.metadata.ItemException;
import net.oneandone.sushi.metadata.Type;
import net.oneandone.sushi.metadata.Variable;
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
