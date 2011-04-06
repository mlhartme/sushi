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

package net.sf.beezle.sushi.xml;

/**
 * <p>Xml processing stuff. Not thread-save - every thread should have it's own instance.
 * Creates members lazy because they are resource comsuming. </p>
 */
public class Xml {
    private Builder builder;
    private Selector selector;
    private Serializer serializer;
    
    public Xml() {
        this.builder = null;
        this.selector = null;
        this.serializer = null;
    }

    public Builder getBuilder() {
        if (builder == null) {
            builder = new Builder();
        }
        return builder;
    }

    public Selector getSelector() {
        if (selector == null) {
            selector = new Selector();
        }
        return selector;
    }

    public Serializer getSerializer() {
        if (serializer == null) {
            serializer = new Serializer();
        }
        return serializer;
    }
}
