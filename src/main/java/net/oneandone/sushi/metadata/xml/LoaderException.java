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
