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

import org.w3c.dom.Element;

public class Namespace {
    public static final Namespace EMPTY_NAMESPACE = Namespace.getNamespace("","");
    public static final Namespace XML_NAMESPACE = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");

    private final String prefix;
    private final String uri;

    private Namespace(String prefix, String uri) {
        this.prefix = prefix;
        this.uri = uri;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getUri() {
        return uri;
    }

    public boolean hasUri(String other) {
        return uri.equals(uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Namespace) {
            return uri.equals(((Namespace)obj).uri);
        }
        return false;
    }

    public ChildElements childElements(Element parent, String childLocalName) {
        return new ChildElements(parent, childLocalName, this);
    }

    
    public static Namespace getNamespace(String prefix, String uri) {
        if (prefix == null) {
            prefix = EMPTY_NAMESPACE.getPrefix();
        }
        if (uri == null) {
            uri = EMPTY_NAMESPACE.getUri();
        }
        return new Namespace(prefix, uri);
    }
}