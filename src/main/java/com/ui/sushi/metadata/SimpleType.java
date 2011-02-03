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

package de.ui.sushi.metadata;

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
