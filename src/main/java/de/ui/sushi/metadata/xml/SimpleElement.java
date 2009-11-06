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

package de.ui.sushi.metadata.xml;

import java.util.List;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import de.ui.sushi.metadata.Item;
import de.ui.sushi.metadata.SimpleType;
import de.ui.sushi.metadata.SimpleTypeException;
import de.ui.sushi.metadata.Type;

public class SimpleElement extends Element {
    private final StringBuilder builder;
    private final SimpleType type;

    public SimpleElement(Item<?> owner, SimpleType type) {
        super(owner);

        this.builder = new StringBuilder();
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Item<?> lookup(String name) {
        return null;
    }

    @Override
    public void addChild(Item<?> item, Object child) {
        throw new IllegalStateException();
    }

    @Override
    public boolean addContent(char[] ch, int ofs, int len) {
        builder.append(ch, ofs, len);
        return true;
    }

    @Override
    public boolean isEmpty() {
        return builder.length() == 0;
    }

    @Override
    public Object create(List<SAXException> exceptions, Locator locator) {
        String str;
        
        str = builder.toString();
        try {
            return type.stringToValue(str);
        } catch (SimpleTypeException e) {
            exceptions.add(new SAXLoaderException("cannot set simple value '" + str + "': " + e.getMessage(), locator));
            return type.newInstance();
        }
    }
}
