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

package com.oneandone.sushi.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import com.oneandone.sushi.fs.console.ConsoleNode;
import com.oneandone.sushi.fs.file.FileFilesystem;
import com.oneandone.sushi.fs.file.FileNode;
import com.oneandone.sushi.fs.memory.MemoryNode;
import com.oneandone.sushi.fs.webdav.WebdavNode;
import com.oneandone.sushi.fs.zip.ZipNode;
import com.oneandone.sushi.util.Reflect;

public class IOTest {
    // TODO
    private static Root fs(IO io) {
        return io.getWorking().getRoot();
    }

    //-- filesystems


    @Test(expected=IllegalArgumentException.class)
    public void filesystemDuplicate() throws IOException {
        IO io = new IO();

        io.addFilesystem(new FileFilesystem(io, "file"));
        io.addFilesystem(new FileFilesystem(io, "file"));
    }

    @Test
    public void filesystemParse() throws Exception {
        Node node;
        IO io;

        io = new IO();
        node = io.node("file:/usr");
        assertEquals("usr", node.getPath());
        // TODO node = io.node("http://heise.de");
        node = io.node("http://heise.de/");
        assertTrue(node instanceof WebdavNode);
        node = io.node("console:///");
        assertTrue(node instanceof ConsoleNode);
        node = io.node("mem://1/foo");
        assertTrue(node instanceof MemoryNode);
    }

    @Test
    public void file() throws IOException {
        IO io;

        io = new IO();
        assertEquals("/foo", io.file("/foo").getFile().getPath());
        assertEquals("/foo", io.file("/foo/").getFile().getPath());
        assertEquals("/", io.file("/").getFile().getPath());
    }

    @Test
    public void nodeForUri() throws IOException, URISyntaxException {
        URI uri;
        Node node;
        IO io;

        io = new IO();
        uri = new URI("http://foo.bar/foo");
        node = io.node(uri);
        assertTrue(node instanceof WebdavNode);
        assertEquals("foo", node.getPath());
        assertEquals(uri.toString(), ((WebdavNode) node).getUrl());

        uri = new URI("file:/home/mhm/bar.txt");
        node = io.node(uri);
        assertTrue(node instanceof FileNode);
        assertEquals("home/mhm/bar.txt", node.getPath());

        uri = getClass().getClassLoader().getResource("java/lang/Object.class").toURI();
        node = io.node(uri);
        assertTrue(node instanceof ZipNode);
        assertEquals("java/lang/Object.class", node.getPath());
    }

    //--

    @Test
    public void nodeRoot() throws Exception {
        IO io;
        Node node;

        io = new IO();
        node = io.node(fs(io).getId());
        assertEquals("", node.getName());
        assertEquals(fs(io).getId(), node.getAbsolute());
        assertEquals(".", node.getRelative(node));
        assertTrue(node.isDirectory());
    }

    @Ignore
    public void nodeAbsolute() throws Exception {
        IO io;
        Node node;

        io = new IO();
        node = io.node(fs(io).getId() + "a");
        assertNull(node.getBase());
        assertEquals("a", node.getName());
        assertEquals("a", node.getPath());
        assertEquals("", node.getParent().getPath());
        assertEquals(fs(io).getId() + "a", node.toString());
    }

    @Ignore
    public void nodeAbsoluteSubdir() throws Exception {
        IO io;
        Node node;

        io = new IO();
        node = io.node(fs(io).getId() + "x" + fs(io).getFilesystem().getSeparator() + "y");
        assertNull(node.getBase());
        assertEquals("y", node.getName());
        assertEquals("x" + fs(io).getFilesystem().getSeparator() + "y", node.getPath());
        assertEquals("x", node.getParent().getPath());
        assertEquals(fs(io).getId() + "x" + fs(io).getFilesystem().getSeparator() + "y", node.toString());
    }


    @Ignore
    public void nodeRelative() throws Exception {
        IO io;
        Node node;

        io = new IO();
        node = io.node("a");
        assertNotNull(node.getBase());
        assertTrue(node.getPath().endsWith(fs(io).getFilesystem().getSeparator() + "a"));
        assertEquals(io.getWorking(), node.getParent());
        assertEquals("a", node.toString());
    }

    @Ignore
    public void nodeDot() throws Exception {
        IO io;
        Node dot;

        io = new IO();
        dot = io.node(".");
        assertNotNull(dot.getBase());
        assertEquals(io.getWorking(), dot);
        assertFalse(".".equals(dot.getName()));
    }

    @Ignore
    public void nodeEmpty() throws Exception {
        IO io;

        io = new IO();
        assertEquals(io.node(""), io.node("."));
    }

    //

    @Ignore
    public void path() throws Exception {
        IO io;
        List<Node> path;

        io = new IO();
        assertEquals(0, io.path("").size());
        path = io.path("foo" + io.os.listSeparator + fs(io).getId() + "bar");
        assertEquals(2, path.size());
        assertEquals("foo", path.get(0).toString());
        assertEquals(io.getWorking().getRoot().getId() + "bar", path.get(1).toString());
        try {
            io.classpath("nosuchfile.jar");
            fail();
        } catch (IOException e) {
            // ok
        }
    }

    //--

    @Test
    public void locate() throws IOException {
        IO io;

        io = new IO();
        io.locateClasspathItem(IO.class).checkDirectory();
        io.locateClasspathItem(Reflect.resourceName(IOTest.class)).checkDirectory();
        io.locateClasspathItem(Object.class).checkFile();
        io.locateClasspathItem("/java/lang/Object.class").checkFile();
        io.locateClasspathItem("/java/lang/Object.class").checkFile();
        assertEquals("foo%20bar.jar", io.locateClasspathItem(new URL("jar:file:/foo%20bar.jar!/some/file.txt"), "/some/file.txt").getPath());
        assertEquals("foo+bar.jar", io.locateClasspathItem(new URL("jar:file:/foo+bar.jar!/some/file.txt"), "/some/file.txt").getPath());
        try {
            assertNull(io.locateClasspathItem("/nosuchresource"));
            fail();
        } catch (RuntimeException e) {
            // ok
        }
    }

    //--

    @Test
    public void fileResource() throws Exception {
        Node node;

        node = new IO().resource("testresource");
        assertTrue(node instanceof FileNode);
        assertTrue(node.isFile());
        assertEquals("hello", node.readString());
    }

    @Test
    public void zipResource() throws Exception {
        Node node;

        node = new IO().resource("org/junit/Assert.class");
        assertTrue(node instanceof ZipNode);
        assertTrue(node.isFile());
        assertEquals(node.length(), node.readBytes().length);
    }

    @Test(expected=FileNotFoundException.class)
    public void noneExisting() throws Exception {
        new IO().resource("nosuchresource");
    }

    @Test(expected=IllegalArgumentException.class)
    public void absolutePath() throws Exception {
        new IO().resource("/absolute");
    }


    //--

    @Test
    public void locateRuntime() throws IOException {
        IO io;

        io = new IO();
        io.locateClasspathItem("/java/lang/Object.class").checkExists();
    }

    @Test
    public void locateFromJar() throws IOException {
        IO io;

        io = new IO();
        io.locateClasspathItem("/org/junit/Test.class").checkExists();
    }

    @Test
    public void projectHome() throws IOException {
        check(IO.class);
        check(IOTest.class);
    }

    private void check(Class<?> clazz) throws IOException {
        IO io;
        FileNode home;
        String name;

        io = new IO();
        home = io.guessProjectHome(clazz);
        assertNotNull(home);
        assertTrue(home.join("pom.xml").isFile());
    }
}
