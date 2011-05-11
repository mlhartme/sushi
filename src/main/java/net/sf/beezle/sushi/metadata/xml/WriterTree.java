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

package net.sf.beezle.sushi.metadata.xml;

import net.sf.beezle.sushi.io.OS;

import java.io.IOException;
import java.io.Writer;

public class WriterTree extends Tree {
    private final Writer dest;
    private int indent;
    private boolean strict;
    private final String lineSeparator;

    public WriterTree(Writer dest, boolean strict) {
    	this(dest, strict, OS.CURRENT.lineSeparator);
    }
    
    public WriterTree(Writer dest, boolean strict, String lineSeparator) {
        this.dest = dest;
        this.indent = 0;
        this.strict = strict;
        this.lineSeparator = lineSeparator;
    }

    @Override
    public Writer done() throws IOException {
        if (indent != 0) {
            throw new IllegalStateException("" + indent);
        }
        dest.flush();
        return dest;
    }

    @Override
    public void ref(String name, int id) throws IOException {
        indent();
        dest.write("<");
        dest.write(name);
        dest.write(" idref='");
        dest.write(Integer.toString(id));
        dest.write("'/>");
        dest.write(lineSeparator);
    }

    @Override
    public void begin(String name, int id, String type, boolean withEnd) throws IOException {
        indent();
        dest.write("<");
        dest.write(name);
        if (id != -1) {
            dest.write(" id='");
            dest.write(Integer.toString(id));
            dest.write('\'');
        }
        type(type);
        if (withEnd) {
            dest.write("/>");
        } else {
            indent++;
            dest.write(">");
        }
        dest.write(lineSeparator);
    }

    @Override
    public void end(String name) throws IOException {
        --indent;
        indent();
        dest.write("</");
        dest.write(name);
        dest.write('>');
        dest.write(lineSeparator);
    }

    @Override
    public void text(String name, String typeAttribute, String text) throws IOException {
        indent();
        dest.write('<');
        dest.write(name);
        type(typeAttribute);
        dest.write('>');
        dest.write(net.sf.beezle.sushi.xml.Serializer.escapeEntities(text, strict));
        dest.write("</");
        dest.write(name);
        dest.write('>');
        dest.write(lineSeparator);
    }

    private void type(String type) throws IOException {
        if (type != null) {
            dest.write(" type='");
            dest.write(type);
            dest.write('\'');
        }
    }
    
    private void indent() throws IOException {
        for (int i = 0; i < indent; i++) {
            dest.write("  ");
        }
    }
}
