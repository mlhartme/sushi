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
    public static ArgumentField create(String name, Schema metadata, Field field) {
        Type type;
        
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException(field + ": static not allowed");
        }
        type = metadata.type(field.getType());
        if (type instanceof SimpleType) {
            return new ArgumentField(name, (SimpleType) type, field);
        } else {
            throw new IllegalArgumentException("argument type not supported: " + field.getType());
        }
    }
    
    //--

    private final Field field;
    
    public ArgumentField(String name, SimpleType simple, Field field) {
        super(name, simple);
        this.field = field;
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
