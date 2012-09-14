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

public class CharacterType extends SimpleType {
    public CharacterType(Schema schema) {
        super(schema, Character.class, "char");
    }
    
    @Override
    public Object newInstance() {
        return 0;
    }

    @Override
    public String valueToString(Object obj) {
        return obj.toString();
    }
    
    @Override
    public Object stringToValue(String str) {
        return str.charAt(0); // TODO
    }
}
