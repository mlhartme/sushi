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

package de.ui.sushi.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GraphTest {
    private Graph<String> g;

    @Before
    public void setUp() {
        g = new Graph<String>();
    }

    @After
    public void after() {
        g.check();
    }
    
    //--
    
    @Test
    public void nodes() {
        assertTrue(g.addNode("foo"));
        assertFalse(g.addNode("foo"));
        assertEquals(1, g.getNodeCount());
        assertTrue(g.addNode("bar"));
        assertEquals(2, g.getNodeCount());
    }
    
    @Test
    public void edge() {
        assertFalse(g.removeEdge("foo", "bar"));
        assertTrue(g.addEdge("foo", "bar"));
        assertTrue(g.containsEdge("foo", "bar"));
        assertFalse(g.addEdge("foo", "bar"));
        assertTrue(g.addEdge("bar", "foo"));
        assertEquals(2, g.getNodeCount());
        assertTrue(g.removeEdge("foo", "bar"));
        assertFalse(g.containsEdge("foo", "bar"));
        assertTrue(g.removeEdge("bar", "foo"));
        assertFalse(g.removeEdge("bar", "foo"));
        assertFalse(g.edges().step());
    }

    @Test
    public  void directCycle() {
    	g.addEdge("a", "a");
    	assertEquals(1, g.removeDirectCycles());
    	assertFalse(g.edges().step());
    	assertEquals(1, g.getNodeCount());
    }
    
    @Test
    public void edges() {
        assertFalse(g.addEdges("foo"));
        assertEquals(0, g.getNodeCount());
        g.addEdges("foo", "bar", "baz");
        assertEquals(3, g.getNodeCount());
        assertTrue(g.containsEdge("foo", "bar"));
        assertTrue(g.containsEdge("foo", "baz"));
    }

    @Test
    public void contains() {
        assertFalse(g.contains("a"));
        assertFalse(g.containsEdge("a", "b"));
        g.addNode("a");
        assertTrue(g.contains("a"));
        assertFalse(g.containsEdge("a", "b"));
        g.addEdge("a", "b");
        assertTrue(g.contains("a"));
        assertTrue(g.containsEdge("a", "b"));
    }

    //--
    
    @Test
    public void sortEmpty() throws CyclicDependency {
        sort();
    }

    @Test
    public void sortIsolated() throws CyclicDependency {
        g.addNode("a");
        g.addNode("b");
        sort("a", "b");
    }
    @Test
    public void sortIsolatedAgain() throws CyclicDependency {
        g.addNode("z");
        g.addNode("a");
        sort("z", "a");
    }

    @Test
    public void sortTransitive() throws CyclicDependency {
        g.addEdge("a", "b");
        g.addEdge("b", "c");
        sort("a", "b", "c");
    }

    @Test
    public void sortTransitiveMore() throws CyclicDependency {
        g.addEdge("a", "b");
        g.addEdge("b", "c");
        g.addEdge("c", "d");
        sort("a", "b", "c", "d");
    }

    @Test
    public void sortFork() throws CyclicDependency {
        g.addEdge("a", "b");
        g.addEdge("a", "c");
        g.addEdge("b", "c");
        sort("a", "b", "c");
    }

    @Test(expected = CyclicDependency.class)
    public void sortCycle() throws CyclicDependency {
        g.addEdge("a", "a");
        sort();
    }

    @Test(expected = CyclicDependency.class)
    public void sortIndirectCycle() throws CyclicDependency {
        g.addEdge("a", "b");
        g.addEdge("b", "a");
        sort();
    }

    @Test
    public void sortTriangle() throws CyclicDependency {
        g.addEdge("a", "b");
        g.addEdge("b", "c");
        g.addEdge("a", "c");
        sort("a", "b", "c");
    }

    private void sort(String ... strings) throws CyclicDependency {
        assertEquals(Arrays.asList(strings), g.sort());
    }

    //--
    
    @Test
    public void graph() {
        Graph<String> op;
        
        op = new Graph<String>();
        g.addGraph(op);
        assertEquals(0, g.getNodeCount());
        op.addNode("foo");
        g.addGraph(op);
        assertEquals(1, g.getNodeCount());
        assertTrue(g.contains("foo"));
        op.addEdge("foo", "bar");
        g.addGraph(op);
        assertTrue(g.containsEdge("foo", "bar"));
    }
    
    //--
    
    @Test
    public void closure() {
        g.addEdge("a", "b");
        closure("a",  "a", "b");
    }

    
    @Test
    public void closureOne() {
        g.addEdge("a", "b");
        g.addEdge("a", "c");
        closure("a",  "a", "b", "c");
        closure("b",  "b");
    }

    @Test
    public void closureTwo() {
        g.addEdge("a", "b");
        g.addEdge("c", "d");
        closure("a",  "a", "b");
        closure("b",  "b");
    }

    private void closure(String start, String ... expected) {
        assertEquals(Arrays.asList(expected), g.closure(start));
    }
    
    //--
    
    @Test
    public void edgeIterator() {
        EdgeIterator<String> iter;
        
        iter = g.edges();
        assertFalse(iter.step());
        assertFalse(iter.step());
        g.addNode("a");
        g.addNode("b");
        iter = g.edges();
        assertFalse(iter.step());
        assertFalse(iter.step());

        g.addEdge("c", "d");
        iter = g.edges();
        assertTrue(iter.step());
        assertEquals("c", iter.left());
        assertEquals("d", iter.right());
        assertFalse(iter.step());

        g.addEdge("a", "b");
        iter = g.edges();
        assertTrue(iter.step());
        assertEquals("a", iter.left());
        assertEquals("b", iter.right());
        assertTrue(iter.step());
        assertEquals("c", iter.left());
        assertEquals("d", iter.right());
        assertFalse(iter.step());
    }
    
    @Test
    public void relationSize() {
        checkSet(g.getDomain());
        checkSet(g.getImage());
        g.addNode("a");
        checkSet(g.getDomain());
        checkSet(g.getImage());
        g.addEdge("a", "b");
        checkSet(g.getDomain(), "a");
        checkSet(g.getImage(), "b");
        g.addEdge("a", "a");
        checkSet(g.getDomain(), "a");
        checkSet(g.getImage(), "a", "b");
    }

    private void checkSet(Set<String> got, String ...expected) {
        assertEquals(new HashSet<String>(Arrays.asList(expected)), got);
        
    }
    
    //--
    
    @Test
    public void string() {
        g.addEdge("a", "b");
        g.addEdge("a", "c");
        assertEquals("[a-b|c, b, c]", g.toString());
    }
    
}
