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

package com.oneandone.sushi.metadata.reflect;

import com.oneandone.sushi.metadata.Cardinality;
import com.oneandone.sushi.metadata.Item;
import com.oneandone.sushi.metadata.Type;

import java.lang.reflect.Field;
import java.util.Collection;

public class CollectionItem extends Item<Object> {
    private final Field field;
    
    public CollectionItem(Field field, Type type) {
        super(field.getName(), Cardinality.SEQUENCE, type, field);

        this.field = field;
    }

    @Override
    public Collection<Object> get(Object src) {
        try {
            return (Collection) field.get(src);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("TODO", e);
        }
    }
    
    @Override
    public void set(Object dest, Collection<Object> values) {
        try {
            // TODO
            field.set(dest, values);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("TODO", e);
        }
    }
}
