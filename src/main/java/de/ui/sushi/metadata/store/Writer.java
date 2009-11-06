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

package de.ui.sushi.metadata.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.ui.sushi.metadata.Cardinality;
import de.ui.sushi.metadata.ComplexType;
import de.ui.sushi.metadata.Item;
import de.ui.sushi.metadata.SimpleType;
import de.ui.sushi.metadata.Type;

/**
 * Helper class to read and write properties. You'll usually not use this class directly,
 * use Type.loadProperties and Data.toProperties instead. 
 */
public class Writer {
    public static void write(Type type, Object obj, String name, final Store dest) {
        new Writer(dest).write(new ArrayList<Item<?>>(), type, obj, name);
    }

    private final Store dest;
    
    public Writer(Store dest) {
        this.dest = dest;
    }

    public Type write(List<Item<?>> parents, Type type, Object obj, String path) {
        if (obj != null) {
            type = type.getSchema().type(obj.getClass());
        }
        writeThis(parents, type, obj, path);
        if (type instanceof ComplexType) {
            ComplexType complex;
            String childName;
            Collection<?> children;
            int idx;

            complex = (ComplexType) type;
            for (Item item : complex.items()) {
                children = item.get(obj);
                parents.add(item);
                idx = 0;
                for (Object child : children) {
                    childName = join(path, item.getName());
                    if (item.getCardinality() == Cardinality.SEQUENCE) {
                        childName = childName + "[" + Integer.toString(idx++) + "]";
                    }
                    write(parents, item.getType(), child, childName);
                }
                parents.remove(parents.size() - 1);
            }
        }
        return type;
    }

    protected void writeThis(List<Item<?>> parents, Type type, Object obj, String path) {
        String value;
        
        if (type instanceof SimpleType) {
            value = ((SimpleType) type).valueToString(obj);
        } else if (obj == null) {
            value = type.getType().getName();
        } else {
            value = obj.getClass().getName(); 
        }
        try {
            dest.write(parents, path, value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new StoreException(path + ": write failed: " + e.getMessage(), e);
        }
    }

    private String join(String first, String second) {
        return first.length() == 0 ? second : first + "/" + second;
    }
}
