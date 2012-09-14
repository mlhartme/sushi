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
package net.oneandone.sushi.fs.webdav;

import net.oneandone.sushi.fs.webdav.methods.Method;
import net.oneandone.sushi.xml.Builder;
import net.oneandone.sushi.xml.ChildElements;
import net.oneandone.sushi.xml.Dom;
import net.oneandone.sushi.xml.Xml;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicLineParser;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MultiStatus {
    private static final String XML_STATUS = "status";
    private static final String XML_PROPSTAT = "propstat";

	public static List<MultiStatus> fromResponse(Xml xml, HttpResponse response) throws IOException {
        Builder builder;
        InputStream in;
		Element root;
		List<MultiStatus> result;
		ChildElements iter;

        in = response.getEntity().getContent();
        try {
            builder = xml.getBuilder();
            synchronized (builder) {
                root = builder.parse(in).getDocumentElement();
            }
        } catch (SAXException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            in.close();
        }
   		Dom.require(root, "multistatus", Method.DAV);
		result = new ArrayList<MultiStatus>();
		iter = Method.DAV.childElements(root, Method.XML_RESPONSE);
		while (iter.hasNext()) {
			fromXml(iter.next(), result);
		}
		return result;
    }

    private static void fromXml(Element response, List<MultiStatus> result) throws IOException {
		Element href;
		String str;
        ChildElements iter;
        Element propstat;
        String propstatStatus;
        Element prop;
        int status;
        ChildElements propIter;

        Dom.require(response, Method.XML_RESPONSE, Method.DAV);
		href = Dom.getFirstChildElement(response, "href", Method.DAV);
        if (href == null) {
            throw new IOException("missing href");
        }
        str = Dom.getString(href).trim();
        iter = Method.DAV.childElements(response, XML_PROPSTAT);
        while (iter.hasNext()) {
            propstat = iter.next();
            propstatStatus = Dom.getString(Dom.getFirstChildElement(propstat, XML_STATUS, Method.DAV));
            prop = Dom.getFirstChildElement(propstat, Method.XML_PROP, Method.DAV);
            status = BasicLineParser.parseStatusLine(propstatStatus, BasicLineParser.DEFAULT).getStatusCode();
            propIter = new ChildElements(prop);
            while (propIter.hasNext()) {
                result.add(new MultiStatus(str, Property.fromXml(propIter.next()), status));
            }
        }
    }

    //--

    public final String href;
    public final Property property;
    public final int status;

    private MultiStatus(String href, Property property, int status) {
        this.href = href;
        this.property = property;
        this.status = status;
    }

    //--

    public static MultiStatus lookup(List<MultiStatus> lst, Name name, int status) {
    	for (MultiStatus ms : lst) {
    		if (status == ms.status && name.equals(ms.property.getName())) {
    			return ms;
    		}
    	}
    	return null;
    }

    public static MultiStatus lookupOne(List<MultiStatus> lst, Name name) {
    	MultiStatus result;

    	result = null;
    	for (MultiStatus ms : lst) {
    		if (name.equals(ms.property.getName())) {
    			if (result != null) {
    				throw new IllegalStateException();
    			}
    			result = ms;
    		}
    	}
    	if (result == null) {
    		throw new IllegalStateException();
    	}
    	return result;
    }
}
