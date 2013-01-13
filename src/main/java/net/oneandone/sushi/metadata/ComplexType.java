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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A list of items */
public class ComplexType extends Type {
    private final List<Item<?>> items;

    public ComplexType(Schema schema, Class<?> type, String name) {
        super(schema, type, name);

        this.items = new ArrayList<>();
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
