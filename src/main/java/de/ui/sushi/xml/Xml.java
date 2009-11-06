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

package de.ui.sushi.xml;

/**
 * <p>Xml processing stuff. Not thread-save - every thread should have it's own instance. </p>
 */
public class Xml {
    public final Builder builder;
    public final Selector selector;
    public final Serializer serializer;
    
    public Xml() {
        this.builder = new Builder();
        this.selector = new Selector();
        this.serializer = new Serializer();
    }
}
