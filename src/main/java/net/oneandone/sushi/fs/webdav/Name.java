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
import net.oneandone.sushi.xml.Namespace;
import org.w3c.dom.Element;

public class Name {
    public static final Name CREATIONDATE = new Name("creationdate", Method.DAV);
    public static final Name DISPLAYNAME = new Name("displayname", Method.DAV);
    public static final Name GETCONTENTLENGTH = new Name("getcontentlength", Method.DAV);
    public static final Name GETLASTMODIFIED = new Name("getlastmodified", Method.DAV);
    public static final Name RESOURCETYPE = new Name("resourcetype", Method.DAV);

    public static Name fromXml(Element nameElement) {
        String ns;

        ns = nameElement.getNamespaceURI();
        if (ns == null) {
            return new Name(nameElement.getLocalName(), Namespace.EMPTY_NAMESPACE);
        } else {
            return new Name(nameElement.getLocalName(), Namespace.getNamespace(nameElement.getPrefix(), ns));
        }
    }

    //--
    
    private final String name;
    private final Namespace namespace;

    public Name(String name, Namespace namespace) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        if (namespace == null) {
            throw new IllegalArgumentException();
        }
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public Namespace getNamespace() {
        return namespace;
    }

    public Element addXml(Element parent) {
    	return Builder.element(parent, name, namespace);
    }
    
    @Override
    public boolean equals(Object obj) {
        Name n;
        
        if (obj instanceof Name) {
            n = (Name) obj;
            return name.equals(n.name) && namespace.equals(n.namespace);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return namespace.equals(Namespace.EMPTY_NAMESPACE) ? name : "{" + namespace.getUri() + "}" + name;
    }
}

