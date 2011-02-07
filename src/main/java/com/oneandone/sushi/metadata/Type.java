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

package com.oneandone.sushi.metadata;

import com.oneandone.sushi.fs.World;
import com.oneandone.sushi.fs.Node;
import com.oneandone.sushi.metadata.store.PropertyStore;
import com.oneandone.sushi.metadata.xml.Loader;
import com.oneandone.sushi.metadata.xml.LoaderException;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public abstract class Type {
    public static final String SCHEMA_HEAD = 
        "<?xml version='1.0' encoding='UTF-8'?>\n" + 
        "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>\n" +
        "  <xs:attributeGroup name='ids'>\n" +
        "    <xs:attribute name='id' type='xs:string'/>\n" +
        "    <xs:attribute name='idref' type='xs:string'/>\n" +
        "  </xs:attributeGroup>\n";
        
    protected final Schema schema;
    protected final Class<?> type;
    protected final String name;

    public Type(Schema schema, Class<?> type, String name) {
        if (type.isPrimitive()) {
            throw new IllegalArgumentException(type.getName());
        }
        if (type.isArray()) {
            throw new IllegalArgumentException(type.getName());
        }
        if (Collection.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException(type.getName());
        }
        this.schema = schema;
        this.type = type;
        this.name = name;
    }

    public Schema getSchema() {
        return schema;
    }
    
    public Class<?> getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }

    
    public abstract Object newInstance();

    public <T> Instance<T> instance(T obj) {
        return new Instance<T>(this, obj);
    }
    
    //--
    
    public <T> Instance<T> loadXml(Node node) throws IOException, LoaderException {
        Reader src;
        Instance<T> result;
        
        src = node.createReader();
        result = loadXml(node.getWorld(), node.getURI().toString(), src);
        src.close();
        return result;
    }

    public <T> Instance<T> loadXml(World world, String systemId, Reader src) throws IOException, LoaderException {
        InputSource input;
        
        input = new InputSource(src);
        input.setSystemId(systemId);
        return loadXml(world, input);
    }

    public <T> Instance<T> loadXml(World world, InputSource src) throws IOException, LoaderException {
        Loader loader;
        T obj;
        
        loader = Loader.create(world, this);
        obj = (T) loader.run(src);
        return instance(obj);
    }

    public <T> Instance<T> loadProperties(Properties props) {
        return loadProperties(props, "");
    }
    
    public <T> Instance<T> loadProperties(Properties props, String name) {
        T obj;
        
        obj = (T) new com.oneandone.sushi.metadata.store.Reader(new PropertyStore(props)).read(name, this);
        return instance(obj);
    }
    
    //--

    
    public List<Type> closure() {
        List<Type> result;
        ComplexType complex;
        Type type;
        
        result = new ArrayList<Type>();
        result.add(this);
        for (int i = 0; i < result.size(); i++) { // result grows!
            type = result.get(i);
            if (type instanceof ComplexType) {
                complex = (ComplexType) type;
                for (Item<?> item : complex.items()) {
                    if (!result.contains(item.getType())) {
                        result.add(item.getType());
                    }
                }
            }
        }
        return result;
    }

    //-- xsd schema generation
    public String createSchema() {
        StringBuilder schema;
        Set<Type> types;

        schema = new StringBuilder();

        schema.append(SCHEMA_HEAD);
        schema.append("  <xs:element name='").append(getName()).append("' type='").append(getSchemaTypeName()).append("'/>\n");

        types = new HashSet<Type>();
        addSchemaType(types, schema);

        schema.append("</xs:schema>");
        return schema.toString();
    }

    public abstract String getSchemaTypeName();
    public abstract void addSchemaType(Set<Type> done, StringBuilder dest);
    
}
