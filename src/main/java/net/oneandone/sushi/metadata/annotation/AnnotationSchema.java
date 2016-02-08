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

import net.oneandone.sushi.metadata.ComplexType;
import net.oneandone.sushi.metadata.Schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Metadata based on annotations */
public class AnnotationSchema extends Schema {
    @Override
    public void complex(ComplexType type) {
        Class<?> clazz;
        int modifier;
        Constructor<?> constr;

        clazz = type.getRawType();
        if (clazz.getAnnotation(net.oneandone.sushi.metadata.annotation.Type.class) == null) {
            throw new IllegalArgumentException("missing type annotation: " + clazz);
        }

        modifier = clazz.getModifiers();
        if (!Modifier.isAbstract(modifier)) {
            if (!Modifier.isPublic(modifier)) {
                throw new IllegalArgumentException(clazz.getName());
            }
            try {
                constr = clazz.getConstructor(new Class[] {});
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(clazz.getName());
            }
            if (!Modifier.isPublic(constr.getModifiers())) {
                throw new IllegalArgumentException(clazz.getName());
            }
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getAnnotation(net.oneandone.sushi.metadata.annotation.Value.class) != null) {
                type.items().add(ValueItem.create(this, field));
            } else if (field.getAnnotation(Option.class) != null) {
                type.items().add(OptionItem.create(this, field));
            } else if (field.getAnnotation(Sequence.class) != null) {
                type.items().add(ListItem.create(this, field));
            }
        }
    }
}
