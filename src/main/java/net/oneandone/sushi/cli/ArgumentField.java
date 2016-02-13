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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ArgumentField extends Argument {
    public static ArgumentField create(Source source, Schema schema, Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException(field + ": static not allowed");
        }
        return new ArgumentField(source, ArgumentType.forReflect(schema, field.getGenericType()), field);
    }
    
    //--

    private final Field field;
    
    public ArgumentField(Source source, ArgumentType type, Field field) {
        super(source, type);
        this.field = field;
    }

    @Override
    public boolean before() {
        return false;
    }

    @Override
    public void doSet(Object obj, Object value) {
        field.setAccessible(true);
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
