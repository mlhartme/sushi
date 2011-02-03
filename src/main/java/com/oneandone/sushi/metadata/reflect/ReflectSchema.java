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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.metadata.ComplexType;
import com.oneandone.sushi.metadata.Item;
import com.oneandone.sushi.metadata.Schema;
import com.oneandone.sushi.metadata.simpletypes.NodeType;

public class ReflectSchema extends Schema {
    public ReflectSchema() {
    }

    public ReflectSchema(IO io) {
        this();
        add(new NodeType(this, io));
    }

    @Override
    public void complex(ComplexType type) {
        Class<?> fieldType;
        Item<?> item;
        
        for (Field field : type.getType().getDeclaredFields()) {
            fieldType = field.getType();
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) {
                item = null;
            } else if (field.isSynthetic()) {
                item = null;  // e.g. this$0
            } else if (fieldType.isArray()) {
                item = new ArrayItem<Object>(field, type(fieldType.getComponentType()));
            } else if (Collection.class.isAssignableFrom(fieldType)) {
                item = new CollectionItem(field, type(Object.class));
            } else {
                item = new ValueItem<Object>(field, type(fieldType));
            }
            if (item != null) {
                type.items().add(item);
            }
        }
    }
}
