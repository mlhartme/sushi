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

package com.oneandone.sushi.metadata.annotation;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import com.oneandone.sushi.metadata.Cardinality;
import com.oneandone.sushi.metadata.Item;
import com.oneandone.sushi.metadata.Schema;
import com.oneandone.sushi.metadata.Type;

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
                lookup(field.getDeclaringClass(), name),
                field);
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
    
    public ListItem(String name, Type type, Method list, AnnotatedElement definition) {
        super(name, Cardinality.SEQUENCE, type, definition);
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
