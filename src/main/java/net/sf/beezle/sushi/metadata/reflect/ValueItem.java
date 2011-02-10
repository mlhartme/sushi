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

package net.sf.beezle.sushi.metadata.reflect;

import net.sf.beezle.sushi.metadata.Cardinality;
import net.sf.beezle.sushi.metadata.Item;
import net.sf.beezle.sushi.metadata.Type;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

public class ValueItem<T> extends Item<T> {
    private final Field field;
    
    public ValueItem(Field field, Type type) {
        super(field.getName(), Cardinality.VALUE, type, field);

        this.field = field;
    }

    @Override
    public Collection<T> get(Object src) {
        T result;
        
        try {
            result = (T) field.get(src);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("TODO", e);
        }
        return Collections.singletonList(result);
    }
    
    @Override
    public void set(Object dest, Collection<T> values) {
        if (values.size() != 1) {
            throw new IllegalArgumentException();
        }
        try {
            field.set(dest, values.iterator().next());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("TODO", e);
        }
    }
}
