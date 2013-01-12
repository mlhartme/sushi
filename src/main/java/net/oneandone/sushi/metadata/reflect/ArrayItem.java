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
package net.oneandone.sushi.metadata.reflect;

import net.oneandone.sushi.metadata.Cardinality;
import net.oneandone.sushi.metadata.Item;
import net.oneandone.sushi.metadata.Type;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class ArrayItem<T> extends Item<T> {
    private final Field field;
    
    public ArrayItem(Field field, Type componentType) {
        super(field.getName(), Cardinality.SEQUENCE, componentType);

        this.field = field;
    }

    @Override
    public Collection<T> get(Object src) {
        T[] array;
        
        try {
            array = (T[]) field.get(src);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("TODO", e);
        }
        return new ArrayList<T>(Arrays.asList(array));
    }
    
    @Override
    public void set(Object dest, Collection<T> values) {
        int max;
        Object arg;
        Iterator<T> iter;
        
        max = values.size();
        arg = Array.newInstance(field.getType().getComponentType(), max);
        iter = values.iterator();
        for (int i = 0; i < max; i++) {
            Array.set(arg, i, iter.next());
        }
        try {
            field.set(dest, arg);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("TODO", e);
        }
    }
}
