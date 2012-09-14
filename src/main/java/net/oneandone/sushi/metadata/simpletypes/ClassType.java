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
package net.oneandone.sushi.metadata.simpletypes;

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;

public class ClassType extends SimpleType {
    public ClassType(Schema schema) {
        super(schema, Class.class, "class");
    }
    
    @Override
    public Object newInstance() {
        return Object.class;
    }

    @Override
    public String valueToString(Object obj) {
        return ((Class) obj).getName();
    }
    
    @Override
    public Object stringToValue(String str) throws SimpleTypeException {
        try {
            return Class.forName(str);
        } catch (ClassNotFoundException e) {
            throw new SimpleTypeException("expected class name, got " + str, e);
        }
    }
}
