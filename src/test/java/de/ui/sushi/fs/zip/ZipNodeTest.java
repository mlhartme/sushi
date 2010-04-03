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

package de.ui.sushi.fs.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.file.FileNode;

/** Accesses external hosts and might need proxy configuration => Full test */
public class ZipNodeTest {
    private IO ioObj = new IO();

    @Test
    public void junit() throws Exception {
        FileNode jar;
        String rootPath;
        String locator;
        ZipNode assrt;
        ZipNode junit;
        Node root;
        List<? extends Node> list;
        List<? extends Node> tree;

        jar = ioObj.locateClasspathItem(Assert.class);
        rootPath = jar.getLocator() + "!/org/junit/Assert.class";
        locator = "jar:" + rootPath;
        assrt = (ZipNode) ioObj.node(locator);
        assertEquals(locator, assrt.getLocator());
        assertEquals("org/junit/Assert.class", assrt.getPath());
        assertTrue(assrt.exists());
        assertTrue(assrt.isFile());
        assertTrue(assrt.readString().length() > 100);
        junit = (ZipNode) assrt.getParent();
        assertEquals("org/junit", junit.getPath());
        assertTrue(junit.isDirectory());
        list = junit.list();
        assertTrue(list.size() > 10);
        assertTrue(list.contains(assrt));
        assertFalse(list.contains(list));
        assertEquals(2, junit.getParent().list().size());
        root = junit.getParent().getParent();
        assertEquals("", root.getPath());
        assertTrue(root.isDirectory());
        assertTrue(root.exists());
        tree = junit.find("**/*");
        assertTrue(tree.size() > list.size());
        assertTrue(tree.contains(assrt));
        assertFalse(tree.contains(list));
        assertTrue(tree.containsAll(list));
        assrt = (ZipNode) junit.join("Assert.class");
        assertTrue(assrt.exists());
        assertTrue(assrt.isFile());
    }

    @Test
    public void readNonexisting() throws IOException {
        FileNode jar;
        Node node;

        jar = ioObj.locateClasspathItem(Object.class);
        node = jar.openZip().getRoot().node("nosuchfile");
        assertFalse(node.exists());
        try {
            node.createInputStream();
            fail();
        } catch (FileNotFoundException e) {
            // ok
        }
    }


    @Test
    public void manifest() throws IOException {
        FileNode jar;

        jar = ioObj.locateClasspathItem(Object.class);
        assertNotNull(jar.openZip().getRoot().readManifest());
    }
}

