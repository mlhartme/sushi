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
package net.oneandone.sushi.metadata.xml;

import net.oneandone.sushi.io.OS;

import java.io.IOException;
import java.io.Writer;

public class WriterTree extends Tree {
    private final Writer dest;
    private int indent;
    private boolean strict;
    private final String lineSeparator;

    public WriterTree(Writer dest, boolean strict) {
    	this(dest, strict, OS.CURRENT.lineSeparator.getSeparator());
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
        dest.write(net.oneandone.sushi.xml.Serializer.escapeEntities(text, strict));
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
