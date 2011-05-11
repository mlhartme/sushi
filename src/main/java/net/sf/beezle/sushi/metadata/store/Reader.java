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

package net.sf.beezle.sushi.metadata.store;

import net.sf.beezle.sushi.metadata.Cardinality;
import net.sf.beezle.sushi.metadata.ComplexType;
import net.sf.beezle.sushi.metadata.Item;
import net.sf.beezle.sushi.metadata.SimpleType;
import net.sf.beezle.sushi.metadata.SimpleTypeException;
import net.sf.beezle.sushi.metadata.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Reader {
    private final Store src;
    
    public Reader(Store src) {
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
