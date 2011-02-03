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

package com.oneandone.sushi.metadata.xml;

import java.io.IOException;
import java.io.Writer;

public class WriterTree extends Tree {
    private final Writer dest;
    private int indent;
    
    public WriterTree(Writer dest) {
        this.dest = dest;
        this.indent = 0;
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
        dest.write("'/>\n");
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
            dest.write("/>\n");
        } else {
            indent++;
            dest.write(">\n");
        }
    }

    @Override
    public void end(String name) throws IOException {
        --indent;
        indent();
        dest.write("</");
        dest.write(name);
        dest.write(">\n");
    }

    @Override
    public void text(String name, String typeAttribute, String text) throws IOException {
        indent();
        dest.write('<');
        dest.write(name);
        type(typeAttribute);
        dest.write('>');
        dest.write(com.oneandone.sushi.xml.Serializer.escapeEntities(text));
        dest.write("</");
        dest.write(name);
        dest.write(">\n");
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
