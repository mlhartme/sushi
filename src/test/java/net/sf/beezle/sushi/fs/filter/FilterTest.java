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
import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.io.OS;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class FilterTest {
    private Node root;

    @Before
    public void setup() throws IOException {
    	root = new World().getTemp().createTempDirectory();
    }

    @Test
    public void emptyIncludes() throws IOException {
        create();
        assertEquals(0, root.find().size());
    }

    @Test
    public void dir() throws IOException {
        create("a/b", "b/c");
        assertEquals(1, root.find("a/b").size());
    }

    @Test
    public void child() throws IOException {
        create("one");
        checkSet(root.find("one"), "one");
    }

    @Test
    public void specialchar() throws IOException {
        create("foo+bar");
        checkSet(root.find("foo+bar"), "foo+bar");
    }

    @Test
    public void children() throws IOException {
        create("a", "b");
        checkSet(root.find("?"), "a", "b");
    }

    @Test
    public void grandChildren() throws IOException {
        List<Node> nodes;

        create("a", "b/c", "b/d");
        nodes = root.find("*/*");
        checkSet(nodes, "b/c", "b/d");
    }

    @Test
    public void star() throws IOException {
        create("1", "2", "3");
        check("*", "1", "2", "3");
    }

    @Test
    public void doubleStar() throws IOException {
        create("a/b");
        check("**/*", "a", "a/b");
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectEmptyPath() throws IOException {
        root.find("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectDoubleStarOnly() {
        root.getWorld().filter().include("**");
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectDoubleDoubleStar() {
        root.getWorld().filter().include("**/**");
    }

    @Test
    public void predicate() throws IOException {
        List<Node> nodes;

        create("a", "b/c", "b/d");
        nodes = root.find(root.getWorld().filter().include("**/*").predicate(Predicate.DIRECTORY));
        assertEquals(1, nodes.size());
        assertEquals(root.join("b"), nodes.get(0));
    }

    @Test
    public void depth() throws IOException {
        create("a", "b/c", "b/d/e");

        check(filter().include("**/*").maxDepth(0) );
        check(filter().include("**/*").maxDepth(1), "a", "b");
        check(filter().include("**/*").minDepth(2).maxDepth(2), "b/c", "b/d");
        check(filter().include("b/*").minDepth(2).maxDepth(2), "b/c", "b/d");
        check(filter().include("**/*").minDepth(3), "b/d/e");
    }

    @Test(expected=IOException.class)
    public void permissionDenied() throws Exception {
    	if (OS.CURRENT != OS.LINUX) {
    		throw new IOException();
    	}
        assertEquals(0, root.getWorld().file("/").find("lost+found/*").size());
    }

    @Test
    public void tree() throws IOException {
        Filter filter;
        TreeAction action;
        Tree tree;
        Tree a;
        Tree b;

        create("a/a", "a/b", "b/a", "b/b");
        filter = filter().include("**/b");
        action = new TreeAction();
        filter.invoke(root, action);
        tree = action.getResult();
        assertEquals(2, tree.children.size());

        a = tree.children.get(0);
        if (a.node.getName().equals("a")) {
        	b = tree.children.get(1);
        } else {
        	b = a;
        	a = tree.children.get(1);
        }
        assertEquals("a", a.node.getName());
        assertEquals(1, a.children.size());
        assertEquals("b", a.children.get(0).node.getName());

        assertEquals("b", b.node.getName());
        assertEquals(1, b.children.size());
        assertEquals("b", b.children.get(0).node.getName());

        action = new TreeAction();
        filter = filter().include("**/c");
        filter.invoke(root, action);
        assertNull(action.getResult());
    }

    //--

    private Filter filter() {
    	return root.getWorld().filter();
    }

    private void create(String... paths) {
        Node file;

        try {
            for (String path : paths) {
                file = root.join(path);
                file.getParent().mkdirsOpt();
                file.writeBytes();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void check(String pattern, String ... paths) throws IOException {
    	checkSet(root.find(pattern), paths);
    }

    private void check(Filter filter, String ... paths) throws IOException {
    	checkSet(root.find(filter), paths);
    }

    private void checkSet(List<Node> nodes, String ... names) {
        assertEquals(names.length, nodes.size());
        for (String name : names) {
            assertTrue(nodes.contains(root.join(name)));
        }
    }
}
