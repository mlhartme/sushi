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

import net.oneandone.sushi.metadata.Item;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;
import net.oneandone.sushi.metadata.Type;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.List;

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
