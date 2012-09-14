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
import net.oneandone.sushi.metadata.Type;
import net.oneandone.sushi.util.Reflect;
import net.oneandone.sushi.xml.Serializer;

import java.util.Set;


public class EnumType extends SimpleType {
    public static EnumType create(Schema schema, Class<? extends Enum> clazz) {
        return new EnumType(schema, clazz, Schema.typeName(clazz), Reflect.getValues(clazz));
    }

    private final Enum[] values;
    
    public EnumType(Schema schema, Class<?> clazz, String name, Enum[] values) {
        super(schema, clazz, name);
        this.values = values;
    }
    
    @Override
    public Object newInstance() {
        return values[0];
    }

    @Override
    public String valueToString(Object obj) {
        return normalizeEnum(obj.toString());
    }
    
    @Override
    public Object stringToValue(String str) throws SimpleTypeException {
        StringBuilder msg;
        String name;
        
        str = normalizeEnum(str);
        msg = new StringBuilder();
        for (Enum e : values) {
            name = normalizeEnum(e.name());
            if (name.equals(str)) {
                return e;
            }
            msg.append(" '");
            msg.append(name);
            msg.append('\'');
        }
        throw new SimpleTypeException("unknown value '" + str + "', expected one of" + msg);
    }

    @Override
    public String getSchemaTypeName() {
        return getName();
    }

    @Override
    public void addSchemaType(Set<Type> done, StringBuilder dest) {
        if (done.contains(this)) {
            return;
        }
        done.add(this);

        dest.append("  <xs:simpleType name='").append(getName()).append("'>\n");
        dest.append("    <xs:restriction base='xs:string' >\n");
        for (Enum e : values) {
            dest.append("    <xs:enumeration value='").append(Serializer.escapeEntities(normalizeEnum(e.toString()), true)).append("'/>\n");
        }
        dest.append("    </xs:restriction>\n");
        dest.append("  </xs:simpleType>\n");
    }

    private static String normalizeEnum(String value) {
        value = value.toLowerCase();
        return value.replace('_', '-');
    }
}
