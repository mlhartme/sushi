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

package net.sf.beezle.sushi.metadata;

import net.sf.beezle.sushi.csv.Csv;
import net.sf.beezle.sushi.csv.View;
import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.NodeWriter;
import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.metadata.store.PropertyStore;
import net.sf.beezle.sushi.metadata.xml.DomTree;
import net.sf.beezle.sushi.metadata.xml.LoaderException;
import net.sf.beezle.sushi.metadata.xml.Serializer;
import net.sf.beezle.sushi.metadata.xml.Tree;
import net.sf.beezle.sushi.metadata.xml.WriterTree;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
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
        NodeWriter writer;
        
        writer = dest.createWriter();
        writer.write("<?xml version='1.0' encoding='");
        writer.write(writer.getEncoding());
        writer.write("'?>\n");
        toXml(writer);
        writer.close();
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

    public Properties toProperties(String name) {
        Properties props;
        
        props = new Properties();
        toProperties(props, name);
        return props;
    }
    
    public void toProperties(Properties props, String name) {
        net.sf.beezle.sushi.metadata.store.Writer.write(getType(), get(), name, new PropertyStore(props));
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
