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

package com.oneandone.sushi.metadata;

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
