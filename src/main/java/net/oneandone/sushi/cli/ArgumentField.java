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
package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ArgumentField extends Argument {
    public static ArgumentField create(Schema schema, Option option, Field field) {
        return create(0, option.value(), schema, 0, 1, field);
    }

    public static ArgumentField create(Schema schema, Value value, Field field) {
        return create(value.position(), value.name(), schema, value.min(), value.max(), field);
    }

    public static ArgumentField create(int position, String name, Schema schema, int minCount, int maxCount, Field field) {
        Type type;
        
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException(field + ": static not allowed");
        }
        type = schema.type(field.getType());
        if (type instanceof SimpleType) {
            return new ArgumentField(position, name, (SimpleType) type, minCount, maxCount, field);
        } else {
            throw new IllegalArgumentException("argument type not supported: " + field.getType());
        }
    }
    
    //--

    private final Field field;
    
    public ArgumentField(int position, String name, SimpleType simple, int min, int max, Field field) {
        super(position, name, simple, min, max);
        this.field = field;
    }

    public boolean before() {
        return false;
    }

    @Override
    public void set(Object obj, Object value) {
        field.setAccessible(true);
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
