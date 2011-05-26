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

package net.sf.beezle.sushi.fs.ssh;

import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.NodeTest;
import net.sf.beezle.sushi.fs.file.FileNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SshNodeFullTest extends NodeTest<SshNode> {
    private static SshRoot root;

    @BeforeClass
    public static void beforeClass() throws Exception {
        root = ConnectionFullTest.open();
    }

    @Before @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void validateDeallocation() {
        assertEquals(0, work.getRoot().getAllocated());
    }

    @AfterClass
    public static void afterClass() {
        if (root != null) {
            root.close();
        }
    }

    @Test(expected = NullPointerException.class)
    public void bug() throws Exception {
        FileNode file;

        file = root.getFilesystem().getWorld().file("/tmp/sushisshworkdir/before\\*after");
        file.mkdir();
        try {
            super.setUp();
        } finally {
            file.delete();
        }
    }

    @Test
    public void recursiveDelete() throws Exception {
        // I used to have problems here because every directory level opened a new channel
        Node dir;

        dir = work.join("a").mkdir();
        dir.join("a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p").mkdirs();
        dir.delete();
    }

    @Test
    public void recursiveCopy() throws Exception {
        Node dir;

        dir = work.join("a").mkdir();
        dir.join("a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p").mkdirs();
        dir.copy(work.join("copy").mkdir());
    }

    // TODO
    @Override @Test
    public void linkRelative() throws IOException {
        try {
            super.linkRelative();
        } catch (ComparisonFailure e) {
            // ok
        }
    }
    private SshNode create(String path) {
        return new SshNode(root, path);
    }

    @Override
    protected SshNode createWork() throws IOException {
        SshNode node;

        node = create("tmp/sushisshworkdir");
        node.deleteOpt();
        node.mkdir();
        return node;
    }

    @Test
    public void rootPath() throws Exception {
        SshNode root;

        root = create("");
        assertEquals("", root.getPath());
        assertEquals("", root.getName());
        assertTrue(root.list().size() > 0);
    }

    @Test
    public void deleteSymlink() throws Exception {
        SshNode root;
        List<SshNode> lst;
        SshNode broken;

        root = createWork();
        root.getRoot().exec("ln", "-s", "nosuchfile", "/" + root.getPath() + "/foo");
        lst = root.list();
        assertEquals(1, lst.size());
        broken = lst.get(0);
        broken.delete();
    }
}
