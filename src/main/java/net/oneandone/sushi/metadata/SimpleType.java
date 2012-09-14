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
package net.oneandone.sushi.metadata;

import java.util.Set;


public abstract class SimpleType extends Type {
    public SimpleType(Schema schema, Class<?> type, String name) {
        super(schema, type, name);
    }

    public abstract String valueToString(Object value);
    
    /** throws an SimpleTypeException to indicate a parsing problem */
    public abstract Object stringToValue(String str) throws SimpleTypeException;

    @Override
    public String getSchemaTypeName() {
        return "xs:" + getName();
    }

    @Override
    public void addSchemaType(Set<Type> done, StringBuilder dest) {
        // type is pre-defined by w3c, nothing to do
    }
}
