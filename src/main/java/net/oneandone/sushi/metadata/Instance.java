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

import net.oneandone.sushi.csv.Csv;
import net.oneandone.sushi.csv.View;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeWriter;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.metadata.properties.Saver;
import net.oneandone.sushi.metadata.xml.DomTree;
import net.oneandone.sushi.metadata.xml.LoaderException;
import net.oneandone.sushi.metadata.xml.Serializer;
import net.oneandone.sushi.metadata.xml.Tree;
import net.oneandone.sushi.metadata.xml.WriterTree;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Some object and its type. TODO: toCsv, fromCsv. */
public class Instance<T> {
    private final Type type;
    private final T instance;
    
    public Instance(Type type, T instance) {
        this.type = type;
        this.instance = instance;
    }

    public Type getType() {
        return type;
    }
    
    public T get() {
        return instance;
    }
    
    @Override
    public Instance<T> clone() {
        Type type;
        StringWriter tmp;
        World world;
        
        world = new World(); // TODO
        type = getType();
        tmp = new StringWriter();
        try {
            toXml(tmp);
            return type.loadXml(world.memoryNode(tmp.getBuffer().toString()));
        } catch (LoaderException e) {
            throw new RuntimeException("invalid!?", e);
        } catch (IOException e) {
            throw new RuntimeException("world exception from memory!?", e);
        }
    }

    @Override
    public String toString() {
        return toXml();
    }
    
    public String valueToString() {
        Type type;
        
        type = getType();
        if (!(type instanceof SimpleType)) {
            throw new IllegalArgumentException("simple type expected: " + type);
        }
        return ((SimpleType) type).valueToString(get());
    }

    //--

    public String toXml() {
        StringWriter writer;
        
        writer = new StringWriter();
        try {
            toXml(writer);
        } catch (IOException e) {
            throw new RuntimeException("unexected", e);
        }
        return writer.toString();
    }

    public void toXml(Node dest) throws IOException {
        try (NodeWriter writer = dest.createWriter()) {
            writer.write("<?xml version='1.0' encoding='");
            writer.write(writer.getEncoding());
            writer.write("'?>\n");
            toXml(writer);
        }
    }
    
    public void toXml(Element parent) throws IOException {
        serialize(new DomTree(parent), Item.xmlName(type.getName()));
    }
    
    public void toXml(Writer dest) throws IOException {
        serialize(new WriterTree(dest, true), type.getName());
    }

    private void serialize(Tree tree, String name) throws IOException {
        Object root;
        List<Object> ids;
        
        root = get();
        ids = Serializer.ids(type, root);
        new Serializer(tree, ids).run(name, type, root);
        tree.done();
    }

    public Properties toProperties() {
        return toProperties("");
    }

    public Properties toProperties(String name) {
        Properties props;
        
        props = new Properties();
        toMap(props, name);
        return props;
    }

    public void toMap(Map<Object, Object> props, String name) {
        Saver.run(getType(), get(), name, props);
    }
    
    public void exportCsv(View view, Csv dest, String ... selection) {
        exportCsv(view, dest, Arrays.asList(selection));
    }

    public void exportCsv(View view, Csv dest, List<String> selection) {
        view.toCsv(this, dest, selection);
    }

    public void importCsv(View view, Csv csv) throws SimpleTypeException {
        view.fromCsv(csv,  this);
    }
}
