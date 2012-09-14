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
package net.oneandone.sushi.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Variable<T> {
    public final Object parent;
    public final Item<T> item;
    
    public Variable(Object parent, Item<T> item) {
        this.parent = parent;
        this.item = item;
    }
    
    public Collection<T> get() {
        return item.get(parent);
    }

    public T getOne() {
        Collection<T> all;
        
        all = get();
        if (all.size() != 1) {
            throw new IllegalStateException("" + all.size());
        }
        return all.iterator().next(); 
    }
    
    public List<String> getStrings() {
        Collection<T> values;
        List<String> result;
        SimpleType simple;
        
        values = get();
        simple = simple();
        result = new ArrayList<String>(values.size());
        for (T value : values) {
            result.add(simple.valueToString(value));
        }
        return result;
    }

    public void set(T values) {
        set(Arrays.asList(values));
    }

    public void set(List<T> values) {
        item.set(parent, values);
    }

    public void setStrings(String ...strings) throws SimpleTypeException {
        setStrings(Arrays.asList(strings));
    }
    
    public void setStrings(List<String> strings) throws SimpleTypeException {
        List<T> values;
        SimpleType simple;
        
        simple = simple();
        values = new ArrayList<T>(strings.size());
        for (String str : strings) {
            values.add((T) simple.stringToValue(str));
        }
        set(values);
    }
    
    private SimpleType simple() {
        Type type;
        
        type = item.getType();
        if (!(type instanceof SimpleType)) {
            throw new IllegalArgumentException("not a simple type: " + type.getName());
        }
        return (SimpleType) type;
    }
}
