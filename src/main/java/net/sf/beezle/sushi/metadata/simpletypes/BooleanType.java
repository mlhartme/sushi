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

package net.sf.beezle.sushi.metadata.simpletypes;

import net.sf.beezle.sushi.metadata.Schema;
import net.sf.beezle.sushi.metadata.SimpleType;
import net.sf.beezle.sushi.metadata.SimpleTypeException;

public class BooleanType extends SimpleType {
    public BooleanType(Schema schema) {
        super(schema, Boolean.class, "boolean");
    }
    
    @Override
    public Object newInstance() {
        return Boolean.FALSE;
    }
    
    @Override
    public String valueToString(Object obj) {
        return obj.toString();
    }
    
    @Override
    public Object stringToValue(String str) throws SimpleTypeException {
        // TODO: because oocalc turns them to upper case
        str = str.toLowerCase();
        if ("true".equals(str)) {
            return Boolean.TRUE;
        } else if ("false".equals(str)) {
            return Boolean.FALSE;
        } else {
            throw new SimpleTypeException("expected true or false, got " + str + ".");
        }
    }
}
