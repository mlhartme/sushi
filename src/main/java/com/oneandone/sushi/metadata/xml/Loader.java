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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import com.oneandone.sushi.fs.IO;
import com.oneandone.sushi.metadata.Item;
import com.oneandone.sushi.metadata.Type;
import com.oneandone.sushi.xml.Builder;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class Loader extends DefaultHandler {
    private Locator locator;
    private final Type type;
    private final SAXParser parser;
    private final List<Element> elements;
    private Object result;
    /** map's id's to Objects */
    private Map<String, Object> storage;
    private List<SAXException> exceptions;

    public static Loader create(IO io, Type type) {
        try {
            // No validation - because it's generally impossible: the complete schema 
            // is unknown because users my specify arbitrary types. Instead, the loader
            // performs proper validation - all unknown elements/attributes are rejected. 
            return new Loader(type, Builder.createSAXParser());
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Loader(Type type, SAXParser parser) {
        this.type = type;
        this.parser = parser;
        this.elements = new ArrayList<Element>();
        this.result = null;
    }

    public Object run(InputSource src) throws IOException, LoaderException {
        elements.clear();

        locator = null;
        storage = new HashMap<String, Object>();
        exceptions = new ArrayList<SAXException>();
        try {
            parser.parse(src, this);
        } catch (SAXParseException e) {
            exceptions.add(e);
        } catch (SAXException e) {
            exceptions.add(e);
        }
        LoaderException.check(exceptions, src, result);
        if (elements.size() != 0) {
            throw new RuntimeException();
        }
        return result;
    }

    //-- SAX parser implementation
    
    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
      
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXLoaderException {
        Element parent;
        Item<?> child;
        Element started;
        int length;
        String name;
        String value;
        String explicitType;
        
        check(uri, localName);
        explicitType = attrs.getValue("type");
        if (elements.size() == 0) {
            // this is the root element - the element name doesn't matter
            started = Element.create(null, override(explicitType, type));
        } else {
            parent = peek();
            child = parent.lookup(qName);
            if (child == null) {
                // cannot recover
                throw new SAXLoaderException("unknown element '" + qName + "'", locator);
            }
            started = Element.create(child, override(explicitType, child.getType()));
        }
        elements.add(started);
        
        length = attrs.getLength();
        for (int i = 0; i < length; i++) {
            name = attrs.getQName(i);
            value = attrs.getValue(i);
            if (name.equals("id")) {
                started.id = value;
            } else if (name.equals("idref")) {
                started.idref = value;
            } else if (name.equals("type")) {
                // already processed
            } else {
                loaderException("unexected attribute " + name);
            }
        }
    }

    private Type override(String explicitType, Type type) {
        if (explicitType != null) {
            try {
                return type.getSchema().type(Class.forName(explicitType));
            } catch (ClassNotFoundException e) {
                loaderException("unknown type: " + explicitType);
                return type;
            }
        } else {
            return type;
        }
    }
    @Override
    public void endElement(String uri, String localName, String qName) {
        Element child;
        Object childObject;

        check(uri, localName);
        child = elements.remove(elements.size() - 1);
        childObject = child.done(storage, exceptions, locator);
        if (elements.size() == 0) {
            result = childObject;
        } else {
            peek().addChild(child.getOwner(), childObject);
        }
    }
    
    @Override
    public void characters(char[] ch, int start, int end) {
        if (!peek().addContent(ch, start, end)) {
            loaderException("unexpected content");
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int end) {
        // ignore
    }

    
    //-- exception handling
    
    @Override
    public void warning(SAXParseException e) {
        exceptions.add(e);
    }

    /** called if the document is not valid */
    @Override
    public void error(SAXParseException e) {
        exceptions.add(e);
    }

    /** called if the document is not well-formed. Clears all other exceptions an terminates parsing */
    @Override
    public void fatalError(SAXParseException e) throws SAXParseException {
        // clear all other exceptions an terminate
        exceptions.clear();
        throw e;
    }

    private void check(String uri, String localName) {
        if (!"".equals(uri)) {
            loaderException("unexpected uri: " + uri);
        }
        if (!"".equals(localName)) {
            loaderException("unexpected localName: " + localName);
        }
    }
    
    /** called for loader exceptions */
    private void loaderException(String msg) {
        exceptions.add(new SAXLoaderException(msg, locator));
    }

    //--

    private Element peek() {
        return elements.get(elements.size() - 1);
    }
}
