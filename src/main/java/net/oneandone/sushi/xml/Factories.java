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

import net.oneandone.sushi.fs.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Create SaxParser- and DocumentBuilder Factories. 
 * Kind of a FactoryFactory to encapsulates xml parser selection.
 * 
 * I have to use jaxp 1.2 (in particular: I cannot use setSchema from jaxp 1.3) because 
 * old Jaxp and Xerces Parsers from JBoss and Tariff/Castor pollute our classpath. 
 * I wasn't able to force Jdk's built-in xerces by directly instantiating the respective 
 * Factory because JBoss didn't find the getSchema method in its endorsed xpi-apis.jar. 
 */
public class Factories {
    private static final Logger LOG = Logger.getLogger(Factories.class.getName());
    
    public static SAXParser saxParser(Node schema) throws IOException {
        SAXParserFactory factory;
        SAXParser parser;
        
        factory = sax();
        factory.setValidating(true);
        factory.setNamespaceAware(false);
        try {
            parser = factory.newSAXParser();
            parser.setProperty(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
            parser.setProperty(JAXP_SCHEMA_SOURCE, new ByteArrayInputStream(schema.readBytes()));
            return parser;
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public static SAXParser saxParser() {
        SAXParserFactory factory;
        
        factory = sax();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        try {
            return factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    public static SAXParserFactory sax() {
        return SAXParserFactory.newInstance();
    }

    
    public static DocumentBuilderFactory document(Node schema) throws IOException, SAXException {
        DocumentBuilderFactory factory;
        Throwable throwable;
        
        factory = document();
        try {
            setJaxp13Validating(factory, schema);
            throwable = null;
        } catch (UnsupportedOperationException | Error e) {
            throwable = e;
        }
        if (throwable != null) {
            LOG.log(Level.FINEST, factory.getClass().getName() + ": JAXP 1.3 validation failed, using 1.2", throwable);
            setJaxp12Validating(factory, schema);
        }
        return factory;
    }

    private static void setJaxp13Validating(DocumentBuilderFactory factory, Node schema) throws IOException, SAXException {
        SchemaFactory sf;
        Source src;
        
        sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        src = new StreamSource(schema.createInputStream());
        factory.setSchema(sf.newSchema(src));
    }

    private static void setJaxp12Validating(DocumentBuilderFactory factory, Node schema) throws IOException {
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        try {
            factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        } catch (IllegalArgumentException x) {
            throw new RuntimeException(factory.getClass().getName() + ": parser does not support JAXP 1.2", x);
        }
        factory.setAttribute(JAXP_SCHEMA_SOURCE, new ByteArrayInputStream(schema.readBytes()));
        
    }
    public static DocumentBuilderFactory document() {
        return DocumentBuilderFactory.newInstance(); 
    }

    //--
    
    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

}
