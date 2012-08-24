/**
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
package net.sf.beezle.sushi.metadata.reflect;

import net.sf.beezle.sushi.metadata.Cardinality;
import net.sf.beezle.sushi.metadata.Item;
import net.sf.beezle.sushi.metadata.Type;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class ArrayItem<T> extends Item<T> {
    private final Field field;
    
    public ArrayItem(Field field, Type componentType) {
        super(field.getName(), Cardinality.SEQUENCE, componentType, field);

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
