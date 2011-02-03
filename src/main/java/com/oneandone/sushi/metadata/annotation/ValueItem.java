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
import java.util.Collections;

import com.oneandone.sushi.metadata.Cardinality;
import com.oneandone.sushi.metadata.Item;
import com.oneandone.sushi.metadata.ItemException;
import com.oneandone.sushi.metadata.Schema;
import com.oneandone.sushi.metadata.Type;

public class ValueItem<T> extends Item<T> {
    public static ValueItem create(Schema metadata, Field field) {
        String name;
        Class type;
        Class fieldType;
        
        name = field.getName();
        type = field.getDeclaringClass();
        fieldType = field.getType();
        return new ValueItem(name, fieldType,  metadata.type(fieldType), 
                lookup(type, "get" + name), lookup(type, "set" + name),
                field);
    }
    
    
    private final Method getter;
    private final Method setter;
    
    public ValueItem(String name, Class typeRaw, Type type, Method getter, Method setter, AnnotatedElement definition) {
        super(name, Cardinality.VALUE, type, definition);
        
        checkSetter(typeRaw, setter);
        checkGetter(typeRaw, getter);
        
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Collection<T> get(Object src) {
        T result;
        
        result = (T) invoke(getter, src);
        return Collections.singletonList(result);
    }
    
    @Override
    public void set(Object dest, Collection<T> values) {
        if (values.size() != 1) {
            throw new ItemException("1 value expected, got " + values.size());
        }
        invoke(setter, dest, values.iterator().next());
    }
}
