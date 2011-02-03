/*
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

package com.oneandone.sushi.metadata.simpletypes;

import java.util.Set;

import com.oneandone.sushi.metadata.Schema;
import com.oneandone.sushi.metadata.SimpleType;
import com.oneandone.sushi.metadata.SimpleTypeException;
import com.oneandone.sushi.metadata.Type;
import com.oneandone.sushi.util.Reflect;
import com.oneandone.sushi.xml.Serializer;


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

        dest.append("  <xs:simpleType name='" + getName() + "'>\n");
        dest.append("    <xs:restriction base='xs:string' >\n");
        for (Enum e : values) {
            dest.append("    <xs:enumeration value='" + Serializer.escapeEntities(normalizeEnum(e.toString())) + "'/>\n");
        }
        dest.append("    </xs:restriction>\n");
        dest.append("  </xs:simpleType>\n");
    }

    private static String normalizeEnum(String value) {
        value = value.toLowerCase();
        return value.replace('_', '-');
    }
}
