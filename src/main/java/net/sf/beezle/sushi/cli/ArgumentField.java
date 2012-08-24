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
package net.sf.beezle.sushi.cli;

import net.sf.beezle.sushi.metadata.Schema;
import net.sf.beezle.sushi.metadata.SimpleType;
import net.sf.beezle.sushi.metadata.Type;

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
