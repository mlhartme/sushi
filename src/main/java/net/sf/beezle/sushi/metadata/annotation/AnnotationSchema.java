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
package net.sf.beezle.sushi.metadata.annotation;

import net.sf.beezle.sushi.metadata.ComplexType;
import net.sf.beezle.sushi.metadata.Schema;

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

        clazz = type.getType();
        if (clazz.getAnnotation(net.sf.beezle.sushi.metadata.annotation.Type.class) == null) {
            throw new IllegalArgumentException("missing type annotation: " + clazz);
        }

        modifier = clazz.getModifiers();
        if (Modifier.isAbstract(modifier)) {
            constr = null;
        } else {
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
            if (field.getAnnotation(net.sf.beezle.sushi.metadata.annotation.Value.class) != null) {
                type.items().add(ValueItem.create(this, field));
            } else if (field.getAnnotation(Option.class) != null) {
                type.items().add(OptionItem.create(this, field));
            } else if (field.getAnnotation(Sequence.class) != null) {
                type.items().add(ListItem.create(this, field));
            }
        }
    }
}
