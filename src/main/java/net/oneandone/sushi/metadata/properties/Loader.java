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
import net.oneandone.sushi.util.Separator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Helper class to read properties. You'll usually not use this class directly, use Instance.toProperties instead.
 */
public class Loader {
    public static Object run(Properties src, Type type) throws LoadException {
        return run(src, type, "");
    }

    public static Object run(Properties src, Type type, String path) throws LoadException {
        Loader loader;
        Object result;

        loader = new Loader(src);
        result = loader.read(path, type);
        if (!src.isEmpty()) {
            loader.error("unused properties: " + src.keySet());
        }
        if (!loader.errors.isEmpty()) {
            throw new LoadException(result, Separator.RAW_LINE.join(loader.errors));
        }
        return result;
    }

    private final Properties src;
    private final List<String> errors;

    private Loader(Properties src) {
        this.src = src;
        this.errors = new ArrayList<>();
    }

    private Object read(String path, Type type) {
        ComplexType parent;
        String value;
        Object obj;
        String childPath;
        Cardinality card;
        Collection<?> col;
        
        value = eatValue(path);
        if (type instanceof SimpleType) {
            if (value == null) {
                error(path + ": value not found");
                return type.newInstance();
            }
            try {
                return ((SimpleType) type).stringToValue(value);
            } catch (SimpleTypeException e) {
                error(path + ": invalid value '" + value + "': " + e.getMessage());
                return type.newInstance();
            }
        } else {
            if (value != null) {
                try {
                    type = type.getSchema().type(Class.forName(value));
                } catch (ClassNotFoundException e) {
                    error(path + ": class not found: " + value);
                    return type.newInstance();
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
                    error(childPath + ": missing values: expected " + card.min + ", got " + col.size());
                } else if (col.size() > card.max) {
                    error(childPath + ": to many values: expected " + card.max + ", got " + col.size());
                } else {
                    item.set(obj, col);
                }
            }
            return obj;
        }
    }

    private void error(String message) {
        errors.add(message);
    }

    private Collection<?> readIndexed(String path, Type type) {
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

    private Collection<?> readNormal(String path, Type type) {
        if (contains(path)) {
            return Collections.singleton(read(path, type));
        } else {
            return Collections.EMPTY_LIST;
        }
    }
    
    private String eatValue(String path) {
        return (String) src.remove(path);
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
