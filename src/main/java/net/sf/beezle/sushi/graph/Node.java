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

package com.oneandone.sushi.graph;

import java.util.ArrayList;
import java.util.List;

public class Node<T> {
    private static int counter = 0;
    
    private final int hashCode;
    public final T data;
    public final List<Node<T>> starting;
    public final List<Node<T>> ending;
    
    public Node(T data) {
        this.hashCode = counter++;
        this.data = data;
        this.starting = new ArrayList<Node<T>>();
        this.ending = new ArrayList<Node<T>>();
    }
    
    /** Override default implementation to get reproducable results */
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
