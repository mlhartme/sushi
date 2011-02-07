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

package com.oneandone.sushi.fs.ssh;

import com.oneandone.sushi.fs.NodeTest;
import com.oneandone.sushi.fs.file.FileNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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

    @After
    public void after() {
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
