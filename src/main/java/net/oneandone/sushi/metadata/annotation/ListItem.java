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
package net.oneandone.sushi.metadata.annotation;

import net.oneandone.sushi.metadata.Cardinality;
import net.oneandone.sushi.metadata.Item;
import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

public class ListItem<T> extends Item<T> {
    public static ListItem create(Schema metadata, Field field) {
        String name;
        Class<?> elementType;
        String singular;
        
        name = field.getName();
        if (!Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException();
        }
        singular = getSingular(name);
        elementType = field.getAnnotation(Sequence.class).value();
        return new ListItem(singular,  
                metadata.type(elementType), 
                lookup(field.getDeclaringClass(), name));
    }
    
    private static final String[] PLURALS = { "List", "s" };

    public static String getSingular(String name) {
        for (String plural : PLURALS) {
            if (name.endsWith(plural) && !name.equals(plural)) {
                return name.substring(0, name.length() - plural.length());
            }
        }
        throw new IllegalArgumentException("invalid name: " + name);
    }
    
    //--
    
    private final Method list;
    
    public ListItem(String name, Type type, Method list) {
        super(name, Cardinality.SEQUENCE, type);
        if (list.getParameterTypes().length != 0) {
            fail(list);
        }
        if (!Collection.class.isAssignableFrom(list.getReturnType())) {
            fail(list);
        }
        check(list);
        
        this.list = list;
    }

    @Override
    public Collection<T> get(Object dest) {
        return (Collection<T>) invoke(list, dest);
    }

    @Override
    public void set(Object dest, Collection<T> values) {
        Collection<T> collection;
        
        collection = get(dest);
        collection.clear();
        collection.addAll(values);
    }
}
