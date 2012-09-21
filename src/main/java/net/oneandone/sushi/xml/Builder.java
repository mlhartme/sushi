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
package net.oneandone.sushi.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

/** Wraps xml parser to implement the method signatures we need */

public class Builder {
    private final DocumentBuilder builder;

    /** Create a non-validating builder */
    public Builder() {
        this.builder = createDocumentBuilder();
    }

    /** Create a validating builder */
    public Builder(net.oneandone.sushi.fs.Node schema) throws IOException, SAXException {
        this.builder = createValidatingDocumentBuilder(schema);
    }

    /** This method is not called "parse" to avoid confusion with file parsing methods */
    public Document parseString(String text) throws SAXException {
        try {
            return parse(new InputSource(new StringReader(text)));
        } catch (IOException e) {
            throw new RuntimeException("unexpected world exception while reading memory stream", e);
        }
    }

    /** asserts a valid document */
    public Document literal(String text) {
        try {
            return parseString(text);        
        } catch (SAXException e) {
            throw new RuntimeException(text, e);
        }
    }

    public Document parse(net.oneandone.sushi.fs.Node node) throws SAXException, IOException {
        InputSource src;

        try (InputStream stream = node.createInputStream()) {
            src = new InputSource();
            src.setSystemId(node.toString());
            src.setByteStream(stream);
            return parse(src);
        }
    }
    
    public Document parse(Reader reader) throws SAXException, IOException {
        return parse(new InputSource(reader));
    }

    public Document parse(InputStream stream) throws SAXException, IOException {
        return parse(new InputSource(stream));
    }

    public Document parse(InputSource src) throws SAXException, IOException {
        return builder.parse(src);
    }

    //--
    
    public Document createDocument() {
        return builder.newDocument();
    }

    public Document createDocument(String name) {
    	return createDocument(name, null);
    }

    public Document createDocument(String name, Namespace namespace) {
        Document doc;
        
        doc = createDocument();
        doc.appendChild(standaloneElement(doc, name, namespace));
        return doc;
    }
    
    //-- create methods 
    
    public static void add(Element parent, NodeList list) {
        int size;
        int i;

        size = list.getLength();
        for (i = 0; i < size; i++) {
            addNode(parent, list.item(i));
        }
        
    }
    
    public static Element add(Element parent, Element child) {
        return (Element) addNode(parent, child);
    }

    public static Node addNode(Element parent, Node child) {
        Document doc;
        
        doc = parent.getOwnerDocument();
        child = doc.importNode(child, true);
        parent.appendChild(child);
        return child;
    }
    
    /** @return newly created element */
    public static Element element(Element parent, String name) {
    	return element(parent, name, null);
    }

    /** @return newly created element */
    public static Element element(Element parent, String name, Namespace namespace) {
        Element result;
        
        result = standaloneElement(parent.getOwnerDocument(), name, namespace);
        parent.appendChild(result);
        return result;
    }

	public static Element standaloneElement(Document doc, String name, Namespace namespace) {
		if (namespace != null) {
            String prefixname = 
            	Namespace.EMPTY_NAMESPACE.equals(namespace) || Namespace.EMPTY_NAMESPACE.getPrefix().equals(namespace.getPrefix()) ?
                 name : namespace.getPrefix() + ":" + name;
            return doc.createElementNS(namespace.getUri(), prefixname);
        } else {
            return doc.createElement(name);
        }
	}
    
    public static Text text(Element parent, String content) {
        Document doc;
        Text result;
        
        doc = parent.getOwnerDocument();
        result = doc.createTextNode(content);
        parent.appendChild(result);
        return result;
    }

    public static Text textElement(Element parent, String name, String content) {
        return textElement(parent, name, null, content);
    }

    public static Text textElement(Element parent, String name, Namespace namespace, String content) {
        return text(element(parent, name, namespace), content);
    }

    public static void clear(Element root) {
        Node child;
        
        while (true) {
            child = root.getFirstChild();
            if (child == null) {
                break;
            }
            root.removeChild(child);
        }
    }
    
    //-- document builder
    
    public static synchronized DocumentBuilder createDocumentBuilder() {
        DocumentBuilder result;
        try {
            result = FACTORY_NON_VALIDATING.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("createDocumentBuilder failed", e);
        }
        result.setErrorHandler(ERROR_HANDLER);
        return result;
    }

    public static DocumentBuilder createValidatingDocumentBuilder(net.oneandone.sushi.fs.Node schema) throws IOException, SAXException {
        DocumentBuilderFactory factory;
        DocumentBuilder builder;

        factory = Factories.document(schema);
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        builder.setErrorHandler(ERROR_HANDLER);
        return builder;
    }

    public static SAXParser createValidatingSAXParser(net.oneandone.sushi.fs.Node schema) throws IOException {
        return Factories.saxParser(schema);
    }

    public static SAXParser createSAXParser() {
        return Factories.saxParser();
    }

    // TODO: avoid problems with old xerces versions in classpath
    private static final DocumentBuilderFactory FACTORY_NON_VALIDATING;
    private static final ErrorHandler ERROR_HANDLER;
    
    static {
        FACTORY_NON_VALIDATING = Factories.document();
        if (!FACTORY_NON_VALIDATING.isNamespaceAware()) {
            FACTORY_NON_VALIDATING.setNamespaceAware(true);
        }
        if (FACTORY_NON_VALIDATING.isValidating()) {
            FACTORY_NON_VALIDATING.setValidating(false);
        }
        
        ERROR_HANDLER = new ErrorHandler() {
            public void error(SAXParseException exception) throws SAXException {
                report(exception);
            }
            public void fatalError(SAXParseException exception) throws SAXException {
                report(exception);
            }
            public void warning(SAXParseException exception) throws SAXException {
                report(exception);
            }
            private void report(SAXParseException exception) throws SAXException {
                throw exception;
            }
        };
    }
}
