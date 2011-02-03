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
import java.util.Iterator;


public class EdgeIterator<T> {
    /** points behind the current  to the next candidate */
    private final Iterator<Node<T>> lefts;
    private Iterator<Node<T>> rights;

    private T left;
    private T right;
    
    public EdgeIterator(Iterator<Node<T>> lefts) {
        this.lefts = lefts;
        this.rights = null;
    }
    
    public boolean step() {
        Node<T> tmp;
        
        if (rights != null && rights.hasNext()) {
            right = rights.next().data;
            return true;
        } 
        while (lefts.hasNext()) {
            tmp = lefts.next();
            if (tmp.starting.size() > 0) {
                left = tmp.data;
                // TODO
                rights = new ArrayList<Node<T>>(tmp.starting).iterator();
                right = rights.next().data;
                return true;
            }
        }
        return false;
    }

    /** undefined without previously calling step() */
    public T left() {
        return left;
    }

    /** undefined without previously calling step() */
    public T right() {
        return right;
    }
}
