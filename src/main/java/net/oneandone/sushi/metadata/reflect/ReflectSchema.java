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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.ComplexType;
import net.oneandone.sushi.metadata.Item;
import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.simpletypes.FileNodeType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

public class ReflectSchema extends Schema {
    public ReflectSchema() {
    }

    public ReflectSchema(World world) {
        this();
        add(new FileNodeType(this, world));
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
