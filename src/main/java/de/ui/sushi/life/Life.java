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

package de.ui.sushi.life;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.metadata.ComplexType;
import de.ui.sushi.metadata.Schema;
import de.ui.sushi.metadata.annotation.AnnotationSchema;
import de.ui.sushi.metadata.annotation.Sequence;
import de.ui.sushi.metadata.annotation.Type;
import de.ui.sushi.metadata.xml.LoaderException;

@Type
public class Life {
    public static Node file(IO io) {
        return io.getHome().join(".m2/life.xml");
    }

    /** Creates the file if it does not yet exist */
    public static Life load(IO io) throws IOException, LoaderException {
        Node file;
        Life life;
        
        file = file(io);
        if (file.isFile()) {
            life = load(file);
        } else {
            life = new Life();
            life.save(file);
        }
        return life;
    }

    public static Life load(Node file) throws IOException, LoaderException {
        return (Life) TYPE.loadXml(file).get();
    }
    
    public static final Schema SCHEMA = new AnnotationSchema();
    public static final ComplexType TYPE = SCHEMA.complex(Life.class); 

    @Sequence(Jar.class) 
    private final List<Jar> jars;
    
    public Life() {
        this.jars = new ArrayList<Jar>();
    }
    
    public List<Jar> jars() {
        return jars;
    }

    public Jar lookup(Id id) {
        for (Jar jar : jars) {
            if (id.equals(jar.getId())) {
                return jar;
            }
        }
        return null;
    }
    
    public List<Jar> lookupWithoutVersion(Id id) {
        List<Jar> result;
        
        result = new ArrayList<Jar>();
        for (Jar jar : jars) {
            if (jar.getId().equalsWithoutVersion(id)) {
                result.add(jar);
            }
        }
        return result;
    }
    
    public Jar lookup(Node node) throws IOException {
        return lookup(Id.fromNode(node));
    }

    public void save(Node file) throws IOException {
        file.writeString(TYPE.instance(this).toXml());
    }
}
