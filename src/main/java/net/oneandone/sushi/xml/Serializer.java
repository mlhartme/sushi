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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

/* Not thread-save! */
public class Serializer {
    private static final TransformerFactory FACTORY = TransformerFactory.newInstance();

    private final Transformer format;
    private final Transformer dumper;

    public Serializer() {
        this.format = createFormatter();
        this.dumper = createDumper();
    }

    public void format(Node src, net.oneandone.sushi.fs.Node dest) throws IOException {
        serialize(src, dest, true);
    }

    public void dump(Node src, net.oneandone.sushi.fs.Node dest) throws IOException {
        serialize(src, dest, false);
    }

    /** Generates an xml/encoding declaration */
    public void serialize(Node src, net.oneandone.sushi.fs.Node dest, boolean format) throws IOException {
        // don't use Saver to allow transformer to decide about encoding */
        try (OutputStream out = dest.createOutputStream()) {
            serialize(new DOMSource(src), new StreamResult(out), dest.getWorld().getSettings().encoding, format);
        }
    }

    public void serialize(Node src, Result dest, boolean format) throws IOException {
        serialize(new DOMSource(src), dest, format);
    }

    public void serialize(Source src, Result dest, boolean format) throws IOException {
        serialize(src, dest, null, format);
    }

    public void serialize(Source src, Result dest, String encoding, boolean format) throws IOException {
        Transformer transformer;
        Throwable cause;

        if (format) {
            transformer = this.format;
            if (encoding == null) {
                transformer.getOutputProperties().remove(OutputKeys.ENCODING);
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            } else {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
                transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            }
        } else {
            transformer = dumper;
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
            serialize(node, result, true);
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
            // this happens with saxon 6.5.5:
            throw new IllegalStateException(str);
        }
        return str.substring(prefix.length(), str.length() - suffix.length()).trim();
    }

    //--

    /**
     * Same method used for attributes and elements ...
     * See http://www.w3.org/TR/REC-xml/#charsets
     */
    public static String escapeEntities(String str, boolean strict) {
        StringBuilder buffer;
        char ch;
        String msg;
        String entity;

        buffer = null;
        for (int i = 0; i < str.length(); i++) {
            ch = str.charAt(i);
            switch (ch) {
                case '\t':
                case '\n':
                case '\r':
                    entity = null;
                    break;
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
                    if (ch < 32) {
                        msg = "illegal character code " + (int) ch;
                        if (strict) {
                            throw new IllegalArgumentException(msg);
                        }
                        entity = "[" + msg + "]";
                    } else if (ch < 256) {
                        entity = null;
                    } else {
                        entity = "&#" + (int) ch + ";";
                    }
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

    private static final Templates FORMATTER;

    static {
        Source src;

        src = new SAXSource(new InputSource(new StringReader(ID)));
        try {
            FORMATTER = templates(src);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Templates templates(Source src) throws TransformerConfigurationException {
        return FACTORY.newTemplates(src);
    }

    private static synchronized Transformer createDumper() {
        try {
            return FACTORY.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static synchronized Transformer createFormatter() {
        Transformer result;

        try {
            result = FORMATTER.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
        result.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        // TODO: ignored by both jdk 1.4 and 1.5's xalan (honored by Saxon)
        result.setOutputProperty(OutputKeys.INDENT, "yes");
        return result;
    }
}
