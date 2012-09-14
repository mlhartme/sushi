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