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

package com.oneandone.sushi.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

public class Serializer {
    private final Transformer transformer;

    public Serializer() {
        this.transformer = createPrettyPrinter();
    }
    
    /** Generates an xml/encoding declaration */
    public void serialize(Node src, com.oneandone.sushi.fs.Node dest) throws IOException {
        OutputStream out;
        
        // don't use Writer to allow transformer to decide about encoding */
        out = dest.createOutputStream();
        serialize(new DOMSource(src), new StreamResult(out), dest.getIO().getSettings().encoding);
        out.close();
    }

    public void serialize(Node src, Result dest) throws IOException {
        serialize(new DOMSource(src), dest);
    }
    
    public void serialize(Source src, Result dest) throws IOException {
        serialize(src, dest, null);
    }

    public void serialize(Source src, Result dest, String encoding) throws IOException {
        Throwable cause;
        
        if (encoding == null) {
            transformer.getOutputProperties().remove(OutputKeys.ENCODING);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");            
        } else {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");            
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);            
        }
        try {
            transformer.transform(src, dest);
        } catch (TransformerException orig) {
            Throwable last;

            last = orig;
            while (true) {
                if (last instanceof SAXException) {
                    cause = ((SAXException) last).getException();
                } else {
                    cause = last.getCause();
                }
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause == null) {
                    break;
                } else {
                    last = cause;
                }
            }
            throw new RuntimeException("unexpected problem with identity transformer", orig);
        }
    }

    //-- to strings

    /**  does not genereate encoding headers */
    public String serialize(Node node) {
        Result result;
        StringWriter dest;

        if (node == null) {
            throw new IllegalArgumentException();
        }
        dest = new StringWriter();
        result = new StreamResult(dest);
        try {
            serialize(node, result);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return dest.getBuffer().toString();
    }

    public String serializeChildren(Document doc) {
        return serializeChildren(doc.getDocumentElement());
    }
    
    public String serializeChildren(Element element) {
        String str;
        String prefix;
        String suffix;
        String root;
        
        root = element.getTagName();
        str = serialize(element).trim();
        prefix = "<" + root + ">"; 
        suffix = "</" + root + ">"; 
        if (!str.startsWith(prefix) || !str.endsWith(suffix)) {
            if (str.equals("<" + root + "/>")) {
                return "";
            } 
            throw new IllegalStateException(str);
        }
        return str.substring(prefix.length(), str.length() - suffix.length()).trim();
    }

    //--
    
    // same method used for attributes and elements ...
    public static String escapeEntities(String str) {
        StringBuilder buffer;
        char ch;
        String entity;

        buffer = null;
        for (int i = 0; i < str.length(); i++) {
            ch = str.charAt(i);
            switch (ch) {
                case '<' :
                    entity = "&lt;";
                    break;
                case '>' :
                    entity = "&gt;";
                    break;
                case '\'' :
                    entity = "&apos;";
                    break;
                case '\"' :
                    entity = "&quot;";
                    break;
                case '&' :
                    entity = "&amp;";
                    break;
                default :
                    entity = null;
                    break;
            }
            if (buffer == null) {
                if (entity != null) {
                    buffer = new StringBuilder(str.length() + 5);
                    buffer.append(str.substring(0, i));
                    buffer.append(entity);
                }
            } else {
                if (entity == null) {
                    buffer.append(ch);
                } else {
                    buffer.append(entity);
                }
            }
        }
        return buffer == null ? str : buffer.toString();
    }
    
    //--

    // pretty-print script by M. Kay, see
    // http://www.cafeconleche.org/books/xmljava/chapters/ch17s02.html#d0e32721
    private static final String ID =
        "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='1.0'>" +
        "  <xsl:output method='xml' indent='yes'/>" +
        "  <xsl:strip-space elements='*'/>" +
        "  <xsl:template match='/'>" +
        "    <xsl:copy-of select='.'/>" +
        "  </xsl:template>" +
        "</xsl:stylesheet>";

    private static final Templates TEMPLATES;
    
    static {
        Source src;

        src = new SAXSource(new InputSource(new StringReader(ID)));
        try {
            TEMPLATES = templates(src);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Templates templates(Source src) throws TransformerConfigurationException {
        // CAUTION: Always use Jre's xalan because Saxon 6.5.x fails in serializeChildren 
        return new com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl().newTemplates(src);
    }
    
    private static synchronized Transformer createPrettyPrinter() {
        Transformer result;

        try {
            result = TEMPLATES.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        result.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        // TODO: ignored by both jdk 1.4 and 1.5's xalan (honored by Saxon)
        result.setOutputProperty(OutputKeys.INDENT, "yes");  
        return result;
    }
}
