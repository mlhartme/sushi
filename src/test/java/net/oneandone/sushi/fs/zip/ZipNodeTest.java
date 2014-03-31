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
package net.oneandone.sushi.fs.zip;

import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.LengthException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Accesses external hosts and might need proxy configuration =&gt; Full test */
public class ZipNodeTest {
    private final World world = new World();

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

        jar = world.locateClasspathItem(Assert.class);
        rootPath = jar.getURI().toString() + "!/org/junit/Assert.class";
        locator = "jar:" + rootPath;
        assrt = (ZipNode) world.node(locator);
        assertEquals(locator, assrt.getURI().toString());
        assertEquals("org/junit/Assert.class", assrt.getPath());
        assertTrue(assrt.exists());
        assertTrue(assrt.isFile());
        assertTrue(assrt.readString().length() > 100);
        junit = assrt.getParent();
        assertEquals("org/junit", junit.getPath());
        assertTrue(junit.isDirectory());
        list = junit.list();
        assertTrue(list.size() > 10);
        assertTrue(list.contains(assrt));
        assertFalse(list.contains(list));
        assertEquals(1, junit.getParent().list().size());
        root = junit.getParent().getParent();
        assertEquals("", root.getPath());
        assertTrue(root.isDirectory());
        assertTrue(root.exists());
        tree = junit.find("**/*");
        assertTrue(tree.size() > list.size());
        assertTrue(tree.contains(assrt));
        assertFalse(tree.contains(list));
        assertTrue(tree.containsAll(list));
        assrt = junit.join("Assert.class");
        assertTrue(assrt.exists());
        assertTrue(assrt.isFile());
    }

    @Test
    public void type() throws Exception {
        FileNode jar;
        ZipNode root;
        int count;

        jar = world.locateClasspathItem(Assert.class);
        root = jar.openZip();
        count = 0;
        for (Node node : root.find("**/*")) {
            count++;
            while (true) {
                node = node.getParent();
                if (node == null) {
                    break;
                }
                assertTrue(node.isDirectory());
                assertFalse(node.isFile());
            }
        }
        assertEquals(266, count);
    }

    @Test
    public void getUriWithSpecialChars() throws Exception {
        ZipNode zip;
        URI uri;
        byte[] bytes;

        zip = world.guessProjectHome(getClass()).join("src/test/test.jar").openZip();
        for (Node node : zip.find("**/*")) {
            uri = node.getURI();
            if (node.isFile()) {
                bytes = node.readBytes();
                assertTrue(Arrays.equals(bytes, world.node(uri).readBytes()));
            }
            assertNotNull(node.getURI());
        }
    }

    @Test
    public void jarWithBlank() throws Exception {
        checkSpecialPath("a b", "foo bar.jar");
    }

    @Test
    public void jarWithHash() throws Exception {
        checkSpecialPath("ab#", "X#Y.jar");
    }

    private void checkSpecialPath(String dir, String name) throws IOException {
        final String clazz = "org/junit/Assert.class";
        FileNode jar;
        Node temp;
        Node copy;
        ZipNode zip;

        jar = world.locateClasspathItem(Assert.class);
        temp = world.getTemp().createTempDirectory();
        copy = temp.join(dir).mkdir().join(name);
        jar.copyFile(copy);
        zip = ((FileNode) copy).openZip();
        assertEquals(1, zip.find(clazz).size());
        assertNotNull(world.validNode("zip:" + copy.getURI() + "!/" + clazz).readBytes());
        assertNotNull(world.validNode("jar:" + copy.getURI() + "!/" + clazz).readBytes());
        temp.deleteTree();
    }

    @Test(expected=FileNotFoundException.class)
    public void createInputStreamNoneExisting() throws IOException {
        FileNode jar;
        Node node;

        jar = world.locateClasspathItem(Object.class);
        node = jar.openZip().getRoot().node("nosuchfile", null);
        assertFalse(node.exists());
        node.createInputStream();
    }

    @Test(expected=FileNotFoundException.class)
    public void readBytesNoneExisting() throws IOException {
        FileNode jar;
        Node node;

        jar = world.locateClasspathItem(Object.class);
        node = jar.openZip().getRoot().node("nosuchfile", null);
        assertFalse(node.exists());
        node.readBytes();
    }

    @Test(expected=LengthException.class)
    public void lengthNoneExisting() throws IOException {
        FileNode jar;
        Node node;

        jar = world.locateClasspathItem(Object.class);
        node = jar.openZip().getRoot().node("nosuchfile", null);
        assertFalse(node.exists());
        node.length();
    }

    @Test
    public void manifest() throws IOException {
        FileNode jar;

        jar = world.locateClasspathItem(Object.class);
        assertNotNull(jar.openZip().getRoot().readManifest());
    }
}

