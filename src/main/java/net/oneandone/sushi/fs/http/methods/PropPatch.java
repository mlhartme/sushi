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
package net.oneandone.sushi.fs.http.methods;

import net.oneandone.sushi.fs.http.HttpConnection;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.http.MovedPermanentlyException;
import net.oneandone.sushi.fs.http.MultiStatus;
import net.oneandone.sushi.fs.http.Name;
import net.oneandone.sushi.fs.http.Property;
import net.oneandone.sushi.fs.http.StatusException;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.fs.http.model.StatusCode;
import net.oneandone.sushi.fs.http.model.StatusLine;
import net.oneandone.sushi.xml.Builder;
import net.oneandone.sushi.xml.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.List;

public class PropPatch extends Method<Void> {
    public static void run(HttpNode resource, Property property) throws IOException {
        Xml xml;
        Document document;
        Element set;
        Element prop;

        xml = resource.getWorld().getXml();
        document = xml.getBuilder().createDocument("propertyupdate", DAV);
        set = Builder.element(document.getDocumentElement(), "set" , DAV);
        prop = Builder.element(set, XML_PROP, DAV);
        property.addXml(prop);
        new PropPatch(resource, property).invoke(Method.body(xml.getSerializer(), document));
    }

    private final Name dest;

    private PropPatch(HttpNode resource, Property property) {
        super("PROPPATCH", resource);
        this.dest = property.getName();
    }

    @Override
    public Void process(HttpConnection connection, Response response) throws IOException {
        List<MultiStatus> lst;
        MultiStatus ms;

    	switch (response.getStatusLine().code) {
    	case StatusCode.OK:
    		return null;
        case StatusCode.MOVED_PERMANENTLY:
        	throw new MovedPermanentlyException();
    	case StatusCode.MULTI_STATUS:
    		lst = multistatus(response);
    		ms = MultiStatus.lookupOne(lst, dest);
    		if (ms.status != StatusCode.OK) {
    			throw new StatusException(new StatusLine(StatusLine.HTTP_1_1, ms.status));
    		}
    		return null;
        default:
        	throw new StatusException(response.getStatusLine());
        }
    }
}