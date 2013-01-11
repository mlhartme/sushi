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
package net.oneandone.sushi.metadata.properties;

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
import java.util.Properties;

/**
 * Helper class to read properties. You'll usually not use this class directly, use Instance.toProperties instead.
 */
public class Reader {
    private final Properties src;
    
    public Reader(Properties src) {
        this.src = src;
    }

    public Object read(Type type) throws LoadException {
        return read("", type);
    }

    public Object read(String path, Type type) throws LoadException {
        ComplexType parent;
        String data;
        Object obj;
        String childPath;
        Cardinality card;
        Collection<?> col;
        
        data = readValue(path);
        if (type instanceof SimpleType) {
            if (data == null) {
                throw new LoadException(path + ": value not found");
            }
            try {
                return ((SimpleType) type).stringToValue(data);
            } catch (SimpleTypeException e) {
                throw new LoadException(path + ": invalid value: " + e.getMessage(), e);
            }
        } else {
            if (data != null) {
                try {
                    type = type.getSchema().type(Class.forName(data));
                } catch (ClassNotFoundException e) {
                    throw new LoadException(path + ": class not found: " + data, e);
                }
            } else {
                // type as specified in schema
            }
            parent = (ComplexType) type;
            obj = parent.newInstance();
            for (Item item : parent.items()) {
                childPath = join(path, item.getName());
                card = item.getCardinality();
                if (item.getCardinality() == Cardinality.SEQUENCE) {
                    col = readIndexed(childPath, item.getType());
                } else {
                    col = readNormal(childPath, item.getType());
                }
                if (col.size() < card.min) {
                    throw new LoadException(childPath + ": missing values: expected " + card.min + ", got " + col.size());
                }
                if (col.size() > card.max) {
                    throw new LoadException(childPath + ": to many values: expected " + card.max + ", got " + col.size());
                }
                item.set(obj, col);
            }
            return obj;
        }
    }

    private Collection<?> readIndexed(String path, Type type) throws LoadException {
        List<Object> col;
        String childPath;
        
        col = new ArrayList<>();
        for (int i = 0; true; i++) {
            childPath = path + "[" + Integer.toString(i) + "]";
            if (!contains(childPath)) {
                return col;
            }
            col.add(read(childPath, type));
        }
    }

    private Collection<?> readNormal(String path, Type type) throws LoadException {
        if (contains(path)) {
            return Collections.singleton(read(path, type));
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    private String readValue(String path) {
        return src.getProperty(path);
    }

    private boolean contains(String prefix) {
        String prefixSlash;
        String str;

        prefixSlash = prefix + "/";
        // TODO: expensive
        for (Object key : src.keySet()) {
            str = (String) key;
            if (str.equals(prefix)) {
                return true;
            }
            if (str.startsWith(prefixSlash)) {
                return true;
            }
        }
        return false;
    }

    private static String join(String first, String second) {
        return first.length() == 0 ? second : first + "/" + second;
    }
}
