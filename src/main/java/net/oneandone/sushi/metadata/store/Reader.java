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
package net.oneandone.sushi.metadata.store;

import net.oneandone.sushi.metadata.Cardinality;
import net.oneandone.sushi.metadata.ComplexType;
import net.oneandone.sushi.metadata.Item;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;
import net.oneandone.sushi.metadata.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to read properties. You'll usually not use this class directly, use Instance.toProperties instead.
 */
public class Reader {
    private final PropertyStore src;
    
    public Reader(PropertyStore src) {
        this.src = src;
    }

    public Object read(Type type) throws StoreException {
        return read("", type);
    }

    public Object read(String path, Type type) throws StoreException {
        return read(new ArrayList<Item<?>>(), path, type);
    }
    
    private Object read(List<Item<?>> parents, String path, Type type) throws StoreException {
        ComplexType parent;
        String data;
        Object obj;
        String childPath;
        Cardinality card;
        Collection<?> col;
        
        data = readValue(parents, path);
        if (data == null) {
            throw new StoreException(path + ": value not found");
        }
        if (type instanceof SimpleType) {
            try {
                return ((SimpleType) type).stringToValue(data);
            } catch (SimpleTypeException e) {
                throw new StoreException(path + ": invalid value: " + e.getMessage(), e);
            }
        } else {
            try {
                type = type.getSchema().type(Class.forName(data));
            } catch (ClassNotFoundException e) {
                throw new StoreException(path + ": class not found: " + data, e);
            }
            parent = (ComplexType) type;
            obj = parent.newInstance();
            for (Item item : parent.items()) {
                parents.add(item);
                childPath = join(path, item.getName());
                card = item.getCardinality();
                if (item.getCardinality() == Cardinality.SEQUENCE) {
                    col = readIndexed(parents, childPath, item.getType());
                } else {
                    col = readNormal(parents, childPath, item.getType());
                }
                if (col.size() < card.min) {
                    throw new StoreException(childPath + ": missing values: expected " + card.min + ", got " + col.size());
                }
                if (col.size() > card.max) {
                    throw new StoreException(childPath + ": to many values: expected " + card.max + ", got " + col.size());
                }
                item.set(obj, col);
                parents.remove(parents.size() - 1);
            }
            return obj;
        }
    }

    private Collection<?> readIndexed(List<Item<?>> parents, String path, Type type) throws StoreException {
        List<Object> col;
        String childPath;
        
        col = new ArrayList<Object>();
        for (int i = 0; true; i++) {
            childPath = path + "[" + Integer.toString(i) + "]";
            if (readValue(parents, childPath) == null) {
                return col;
            }
            col.add(read(parents, childPath, type));
        }
    }

    private Collection<?> readNormal(List<Item<?>> parents, String path, Type type) throws StoreException {
        if (readValue(parents, path) != null) {
            return Collections.singleton(read(parents, path, type));
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    private String readValue(List<Item<?>> parents, String path) throws StoreException {
        try {
            return src.read(parents, path);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new StoreException(path + ": read failed: " + e.getMessage(), e);
        }
    }

    private static String join(String first, String second) {
        return first.length() == 0 ? second : first + "/" + second;
    }
}
