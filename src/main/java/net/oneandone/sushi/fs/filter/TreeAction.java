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
package net.oneandone.sushi.fs.filter;

import net.oneandone.sushi.fs.Node;

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
