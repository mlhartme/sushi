/*
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
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.SizeException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Accesses external hosts and might need proxy configuration =&gt; Full test */
public class ZipNodeTest {
    private final World world;

    public ZipNodeTest() throws IOException {
        world = World.create();
    }

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

        jar = world.locateClasspathEntry(Assert.class);
        rootPath = jar.getUri().toString() + "!/org/junit/Assert.class";
        locator = "jar:" + rootPath;
        assrt = (ZipNode) world.node(locator);
        assertEquals(locator, assrt.getUri().toString());
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

        jar = world.locateClasspathEntry(Assert.class);
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
        assertEquals(387, count);
    }

    @Test
    public void getUriWithSpecialChars() throws Exception {
        ZipNode zip;
        URI uri;
        byte[] bytes;

        zip = world.guessProjectHome(getClass()).join("src/test/test.jar").openZip();
        for (Node node : zip.find("**/*")) {
            uri = node.getUri();
            if (node.isFile()) {
                bytes = node.readBytes();
                assertTrue(Arrays.equals(bytes, world.node(uri).readBytes()));
            }
            assertNotNull(node.getUri());
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

        jar = world.locateClasspathEntry(Assert.class);
        temp = world.getTemp().createTempDirectory();
        copy = temp.join(dir).mkdir().join(name);
        jar.copyFile(copy);
        zip = ((FileNode) copy).openZip();
        assertEquals(1, zip.find(clazz).size());
        assertNotNull(world.validNode("zip:" + copy.getUri() + "!/" + clazz).readBytes());
        assertNotNull(world.validNode("jar:" + copy.getUri() + "!/" + clazz).readBytes());
        temp.deleteTree();
    }

    @Test(expected = FileNotFoundException.class)
    public void newInputStreamNoneExisting() throws IOException {
        FileNode jar;
        Node node;

        jar = world.locateClasspathEntry(Test.class);
        node = jar.openZip().getRoot().node("nosuchfile", null);
        assertFalse(node.exists());
        node.newInputStream();
    }

    @Test(expected = FileNotFoundException.class)
    public void readBytesNoneExisting() throws IOException {
        FileNode jar;
        Node node;

        jar = world.locateClasspathEntry(Test.class);
        node = jar.openZip().getRoot().node("nosuchfile", null);
        assertFalse(node.exists());
        node.readBytes();
    }

    @Test(expected = SizeException.class)
    public void lengthNoneExisting() throws IOException {
        FileNode jar;
        Node node;

        jar = world.locateClasspathEntry(Test.class);
        node = jar.openZip().getRoot().node("nosuchfile", null);
        assertFalse(node.exists());
        node.size();
    }

    @Test
    public void manifest() throws IOException {
        FileNode jar;

        jar = world.locateClasspathEntry(Test.class);
        assertNotNull(jar.openZip().getRoot().readManifest());
    }

    //-- test jdk behaviour

    @Test
    public void jdk144() throws IOException {
        ZipRoot root;
        ZipEntry entry;

        if (beforeJdk144()) {
            return;
        }
        root = world.locateClasspathEntry(Assert.class).openZip().getRoot();
        entry = root.getZip().getEntry("org/junit");
        assertEquals("org/junit/", entry.getName());
        assertEquals(0, entry.getSize());
        assertTrue(entry.isDirectory());

        // empty file
        assertEquals(-1, root.getZip().getInputStream(entry).read());

    }

    @Test
    public void jdk141() throws IOException {
        ZipRoot root;
        ZipEntry entry;

        if (!beforeJdk144()) {
            return;
        }
        root = world.locateClasspathEntry(Assert.class).openZip().getRoot();
        entry = root.getZip().getEntry("org/junit");
        assertEquals("org/junit", entry.getName());
        assertEquals(0, entry.getSize());
        assertFalse(entry.isDirectory());

        // empty file
        assertNull(root.getZip().getInputStream(entry));
    }

    private static boolean beforeJdk144() {
        String version;
        int n;

        version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            if (version.startsWith("1.8.0_")) {
                n = Integer.parseInt(version.substring(6));
                return n < 144;
            } else if (version.equals("1.8.0-adoptopenjdk")) {
                return false;
            } else {
                // 1.6, 1.7, ...
                return true;
            }
        } else {
            // Java 9 or 10
            return false;
        }
    }
}
