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
package net.sf.beezle.sushi.metadata.annotation;

import net.sf.beezle.sushi.metadata.Cardinality;
import net.sf.beezle.sushi.metadata.Item;
import net.sf.beezle.sushi.metadata.ItemException;
import net.sf.beezle.sushi.metadata.Schema;
import net.sf.beezle.sushi.metadata.Type;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

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
