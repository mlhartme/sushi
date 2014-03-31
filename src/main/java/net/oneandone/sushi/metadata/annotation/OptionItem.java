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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class OptionItem<T> extends Item<T> {
    public static <T> OptionItem<T> create(Schema metadata, Field field) {
        String name;
        Class<?> type;
        Class<?> fieldType;
        
        name = field.getName();
        type = field.getDeclaringClass();
        fieldType = field.getType();
        return new OptionItem<T>(name, fieldType, metadata.type(fieldType), 
                lookup(type, "get" + name), lookup(type, "set" + name));
    }
    
    
    private final Method getter;
    private final Method setter;
    
    public OptionItem(String name, Class<?> typeRaw, Type type, Method getter, Method setter) {
        super(name, Cardinality.OPTION, type);
        
        if (typeRaw.isPrimitive()) {
            throw new IllegalArgumentException("primitive type is not allowed for options: " + typeRaw);
        }
        checkSetter(typeRaw, setter);
        checkGetter(typeRaw, getter);
        
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Collection<T> get(Object src) {
        T result;
        
        result = (T) invoke(getter, src);
        return result == null ? new ArrayList<T>() : Collections.singletonList(result);
    }
    
    @Override
    public void set(Object dest, Collection<T> values) {
        Object value;
        
        switch (values.size()) {
        case 0:
            value = null;
            break;
        case 1: 
            value = values.iterator().next();
            break;
        default:
            throw new IllegalArgumentException();
        }
        invoke(setter, dest, value);
    }
}
