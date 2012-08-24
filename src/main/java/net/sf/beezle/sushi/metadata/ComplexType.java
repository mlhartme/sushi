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
package net.sf.beezle.sushi.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A list of items */
public class ComplexType extends Type {
    private final List<Item<?>> items;

    public ComplexType(Schema schema, Class<?> type, String name) {
        super(schema, type, name);

        this.items = new ArrayList<Item<?>>();
    }

    public List<Item<?>> items() {
        return items;
    }

    public Item<?> lookupXml(String name) {
        for (Item<?> item : items) {
            if (item.getXmlName().equals(name)) {
                return item;
            }
        }
        return null;
    }
    
    public Item<?> lookup(String name) {
        for (Item<?> item : items) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public Object newInstance() {
        try {
            return type.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    //-- xsd schema generation

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

        dest.append("  <xs:complexType name='").append(getName()).append("'>\n");
        dest.append("    <xs:sequence minOccurs='0'>\n");
        for (Item<?> item : items) {
            dest.append("      <xs:element name='").append(item.getXmlName()).append("' type='").append(item.getType().getSchemaTypeName()).append("'").append(item.getCardinality().forSchema()).append("/>\n");
        }
        dest.append("    </xs:sequence>\n");
        dest.append("    <xs:attributeGroup ref='ids'/>\n");
        dest.append("  </xs:complexType>\n");

        for (Item<?> item : items) {
            item.getType().addSchemaType(done, dest);
        }
    }
}
