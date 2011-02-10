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

package net.sf.beezle.sushi.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A directed graph of nodes (with type T) and edges. */
public class Graph<T> {
    private final Map<T, Node<T>> nodes;
    
    public Graph() {
        this.nodes = new LinkedHashMap<T, Node<T>>();
    }
    
    // nodes
    
    /** @return number of nodes */
    public int getNodeCount() {
        return nodes.size();
    }
    
    public Iterator<T> nodes() {
        return Collections.unmodifiableSet(nodes.keySet()).iterator();
    }

    public boolean contains(T data) {
        return nodes.containsKey(data);
    }
    
    /** Adds data to the Graph and returns true, or does nothing and returns false if data is already part of the graph. */
    public boolean addNode(T data) {
        if (nodes.get(data) == null) {
            doNode(data);
            return true;
        } else {
            return false;
        }
    }
    
    public void retainNodes(List<T> datas) {
        for (T data : new ArrayList<T>(this.nodes.keySet())) {
            if (!datas.contains(data)) {
                doRemove(this.nodes.get(data));
            }
        }
    }
    
    //-- edges
    
    public boolean containsEdge(T left, T right) {
        Node<T> start;

        start = nodes.get(left);
        if (start == null) {
            return false;
        }
        for (Node<T> end : start.starting) {
            if (end.data.equals(right)) {
                return true;
            }
        }
        return false;
    }

    public boolean addEdge(T src, T dest) {
        Node<T> left;
        Node<T> right;
        
        left = doNode(src);
        right = doNode(dest);
        if (left.starting.contains(right)) {
            return false;
        } else {
            left.starting.add(right);
            right.ending.add(left);
            return true;
        }
    }

    public boolean addEdges(T left, T ... rights) {
        return addEdges(left, Arrays.asList(rights));
    }
    
    public boolean addEdges(T left, List<T> rights) {
        boolean modified;
        
        modified = false;
        for (T right : rights) {
            if (addEdge(left, right)) {
                modified = true;
            }
        }
        return modified;
    }

    public EdgeIterator<T> edges() {
        // TODO
        return new EdgeIterator<T>(new ArrayList<Node<T>>(nodes.values()).iterator());
    }

    public boolean removeEdge(T left, T right) {
        Node<T> start;
        Node<T> end;
        
        start = nodes.get(left);
        if (start != null) {
        	end = nodes.get(right);
        	if (end != null && start.starting.remove(end)) {
        		end.ending.remove(start);
        		return true;
        	}
        }
        return false;
    }

    //-- graphs
    
    /**
     * Adds all nodes and edges to his graph.
     * 
     * @return true if this Graph has been modified
     */
    public boolean addGraph(Graph<T> graph) {
        boolean modified;

        modified = false;
        for (Node<T> src : graph.nodes.values()) {
            if (addNode(src.data)) {
                modified = true;
            }
            for (Node<T> dest : src.starting) {
                if (addEdge(src.data, dest.data)) {
                    modified = true;
                }
            }
        }
        return modified;
    }

    
    public int removeDirectCycles() {
    	int count;
    	
    	count = 0;
    	for (T data : nodes.keySet()) {
    		if (removeEdge(data, data)) {
    			count++;
    		}
    	}
    	return count;
    }
    
    /** CAUTION: removes nodes from the graph */
    public List<T> sort() throws CyclicDependency {
        List<T> result;
        Node<T> node;
        
        result = new ArrayList<T>();
        while (nodes.size() > 0) {
            node = findStart();
            if (node == null) {
                throw new CyclicDependency(this);
            }
            doRemove(node);
            result.add(node.data);
        }
        return result;
    }
    
    //--

    // TODO: merge with other closure methods
    public void closureHere() {
        boolean modified;

        // clone collections to cope with modification
        do {
            modified = false;
            for (Node<T> left : new ArrayList<Node<T>>(nodes.values())) {
                for (Node<T> right : new ArrayList<Node<T>>(left.starting)) {
                    for (Node<T> rightright : right.starting) {
                        if (addEdge(left.data, rightright.data)) {
                            modified = true;
                        }
                    }
                }
            }
        } while (modified);
    }

    public List<T> closure(T ... data) {
        List<T> result;
        
        result = new ArrayList<T>(Arrays.asList(data));
        closure(result);
        return result;
    }
    
    public void closure(List<T> result) {
        T data;
        Node<T> node;
        int i;
        
        // size grows!
        for (i = 0; i < result.size(); i++) {
            data = result.get(i);
            node = nodes.get(data);
            if (node == null) {
                throw new IllegalArgumentException("unknown data: " + data);
            }
            for (Node<T> to : node.starting) {
                if (!result.contains(to.data)) {
                    result.add(to.data);
                }
            }
        }
    }
    
    //--  Graph as a relation

    public Set<T> getDomain() {
        Set<T> set;
        
        set = new HashSet<T>();
        getDomain(set);
        return set;
    }
    
    public void getDomain(Collection<T> result) {
        for (Node<T> node : nodes.values()) {
            if (node.starting.size() > 0) {
                result.add(node.data);
            }
        }
    }

    public Set<T> getImage() {
        Set<T> set;
        
        set = new HashSet<T>();
        getImage(set);
        return set;
    }

    public void getImage(Collection<T> result) {
        for (Node<T> node : nodes.values()) {
            if (node.ending.size() > 0) {
                result.add(node.data);
            }
        }
    }

    public String toRelationString() {
        StringBuilder result;
        EdgeIterator<T> iter;
        boolean first;

        result = new StringBuilder();
        result.append("{ ");
        iter = edges();
        first = true;
        while (iter.step()) {
            if (!first) {
                result.append(", ");
                first = true;
            }
            result.append('(');
            result.append(iter.left().toString());
            result.append(',');
            result.append(iter.right().toString());
            result.append(')');
        }
        result.append(" }");
        return result.toString();
    }
    
    //--
    
    public void check() {
        for (Node<T> node : nodes.values()) {
            for (Node<T> tmp : node.starting) {
                if (!tmp.ending.contains(node)) {
                    throw new IllegalStateException();
                }
            }
            for (Node<T> tmp : node.ending) {
                if (!tmp.starting.contains(node)) {
                    throw new IllegalStateException();
                }
            }
        }
    }
    
    //--
    
    private Node<T> findStart() {
        for (Node<T> node : nodes.values()) {
            if (node.ending.size() == 0) {
                return node;
            }
        }
        return null;
    }

    private Node<T> doNode(T data) {
        Node<T> node;
        
        node = nodes.get(data);
        if (node != null) {
            return node;
        }
        node = new Node<T>(data);
        nodes.put(data, node);
        return node;
    }

    private void doRemove(Node<T> node) {
        if (nodes.remove(node.data) != node) {
            throw new IllegalStateException();
        }
        for (Node<T> tmp : node.starting) {
            if (!tmp.ending.remove(node)) {
                throw new IllegalStateException();
            }
        }
        for (Node<T> tmp : node.ending) {
            if (!tmp.starting.remove(node)) {
                throw new IllegalStateException();
            }
        }
    }
    
    public String getNodeNames() {
        StringBuilder builder;
        
        builder = new StringBuilder();
        for (T data : nodes.keySet()) {
            builder.append(toString(data));
            builder.append(' ');
        }
        return builder.toString();
    }
    
    //--
    
    @Override
    public String toString() {
        List<String> items;

        items = new ArrayList<String>();
        for (T data : nodes.keySet()) {
            items.add(toNodeString(data));
        }
        Collections.sort(items);
        return items.toString();
    }
    
    private String toNodeString(T data) {
        StringBuilder builder;
        Node<T> node;
        boolean firstTo;
        
        builder = new StringBuilder();
        node = nodes.get(data);
        builder.append(toString(data));
        if (node.starting.size() > 0) {
            builder.append("-");
            firstTo = true;
            for (Node<T> to : node.starting) {
                if (firstTo) {
                    firstTo = false;
                } else {
                    builder.append('|');
                }
                builder.append(toString(to.data));
            }
        }
        return builder.toString();
    }

    /** You might want to override this method. */
    protected String toString(T data) {
        return data.toString();
    }
}
