/**
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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** A none-empty list of sax exceptions with position information in the message. */
public class LoaderException extends IOException {
    private static final long serialVersionUID = 2L;

    /** 
     * @param lst 
     *     if the document is not well-formed: exactly one none-loader exception;
     *     if the document is well-formed but invalid: both loader and none-loader exceptions
     */
    public static void check(List<SAXException> lst, InputSource src, Object loaded) throws LoaderException {
        StringBuilder builder;
        List<SAXException> loader;
        
        if (lst.size() == 0) {
            return;
        }
        loader = getLoaderExceptions(lst);
        if (loader.size() > 0) {
            // Forget about other exception, consider loader exceptions only
            lst = loader;
        }
        builder = new StringBuilder();
        for (SAXException e : lst) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(msg(e, src));
        }
        throw new LoaderException(builder.toString(), lst, loaded);
    }
    
    private static List<SAXException> getLoaderExceptions(List<SAXException> lst) {
        List<SAXException> result;
        
        result = new ArrayList<SAXException>();
        for (SAXException e : lst) {
            if (e instanceof SAXLoaderException) {
                result.add(e);
            }
        }
        return result;
    }

    //--
    
    private final List<SAXException> causes;
    private final Object loaded;
    
    public LoaderException(String msg, List<SAXException> causes, Object loaded) {
        super(msg);
        this.causes = causes;
        this.loaded = loaded;
    }

    public Object getLoaded() {
        return loaded;
    }
    
    public List<SAXException> causes() {
        return causes;
    }
    
    private static String msg(SAXException e, InputSource src) {
        SAXParseException pe;
        
        if (e instanceof SAXParseException) {
            pe = (SAXParseException) e;
            return pe.getSystemId() + ":" + pe.getLineNumber() + ":" + pe.getColumnNumber() + ": " + pe.getMessage();
        } else {
            return src.getSystemId() + ": " + e.getMessage();
        }
    }
}
