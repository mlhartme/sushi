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

package com.oneandone.sushi.fs.webdav;

import com.oneandone.sushi.fs.webdav.methods.Method;
import com.oneandone.sushi.xml.Builder;
import com.oneandone.sushi.xml.Namespace;
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

