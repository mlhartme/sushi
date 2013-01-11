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

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.metadata.properties.LoadException;
import net.oneandone.sushi.metadata.xml.Loader;
import net.oneandone.sushi.metadata.xml.LoaderException;
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
        return new Instance<>(this, obj);
    }
    
    //--

    public <T> Instance<T> loadXml(Node node) throws IOException, LoaderException {
        // TODO: use stream instead!?
        try (Reader src = node.createReader()) {
            return loadXml(node.getURI().toString(), src);
        }
    }

    public <T> Instance<T> loadXml(String systemId, Reader src) throws IOException, LoaderException {
        InputSource input;
        
        input = new InputSource(src);
        input.setSystemId(systemId);
        return loadXml(input);
    }

    public <T> Instance<T> loadXml(InputSource src) throws IOException, LoaderException {
        Loader loader;
        T obj;
        
        loader = Loader.create(this);
        obj = (T) loader.run(src);
        return instance(obj);
    }

    public <T> Instance<T> loadProperties(Properties props) throws LoadException {
        return loadProperties(props, "");
    }

    public <T> Instance<T> loadProperties(Properties props, String name) throws LoadException {
        T obj;
        
        obj = (T) net.oneandone.sushi.metadata.properties.Loader.run(props, this, name);
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
