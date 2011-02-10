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

package net.sf.beezle.sushi.fs.filter;

import net.sf.beezle.sushi.fs.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TreeAction implements Action {
    private final List<Node> nodes;
    private final List<Tree> trees;
    private Tree result;
    
    public TreeAction() {
        this.nodes = new ArrayList<Node>();
        this.trees = new ArrayList<Tree>();
    }
    
    public void enter(Node node, boolean isLink) {
        Tree parent;
        
        if (trees.size() > 0) {
            parent = trees.get(trees.size() - 1);
            if (parent != null) {
                for (Tree child : parent.children) {
                    if (node == child.node) {
                        nodes.add(node);
                        trees.add(child);
                        return;
                    }
                }
            }
        }
        nodes.add(node);
        trees.add(null);
    }
    
    public void enterFailed(Node node, boolean isLink, IOException e) throws IOException {
        throw e;
    }

    public void leave(Node node, boolean isLink) {
        int idx;
        
        idx = nodes.size() - 1;
        nodes.remove(idx);
        result = trees.remove(idx);
    }
    
    public void select(Node node, boolean isLink) {
        Tree added;
        Tree current;
        
        added = new Tree(node); 
        for (int i = trees.size() - 1; i >= 0; i--) {
            current = trees.get(i);
            if (current == null) {
                current = new Tree(nodes.get(i));
                current.children.add(added);
                trees.set(i, current);
                added = current;
            } else {
                current.children.add(added);
                break;
            }
        }
    }

    public Tree getResult() {
        if (nodes.size() != 0) {
            throw new IllegalStateException();
        }
        if (trees.size() != 0) {
            throw new IllegalStateException();
        }
        return result;
    }
}
