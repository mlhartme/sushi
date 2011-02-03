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

package com.oneandone.sushi.fs.filter;

import java.io.IOException;
import java.util.Collection;

import com.oneandone.sushi.fs.Node;

public class CollectAction implements Action {
    private final Collection<Node> collection;
    
    public CollectAction(Collection<Node> collection) {
        this.collection = collection;
    }
    
    public void enter(Node node, boolean isLink) {
    }

    public void enterFailed(Node node, boolean isLink, IOException e) throws IOException {
        throw e;
    }

    public void leave(Node node, boolean isLink) {
    }
    
    public void select(Node node, boolean isLink) {
        collection.add(node);
    }
}
