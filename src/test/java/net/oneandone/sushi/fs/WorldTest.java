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
package net.oneandone.sushi.fs;

import net.oneandone.sushi.fs.console.ConsoleNode;
import net.oneandone.sushi.fs.file.FileFilesystem;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.http.HttpNode;
import net.oneandone.sushi.fs.memory.MemoryNode;
import net.oneandone.sushi.fs.zip.ZipNode;
import net.oneandone.sushi.util.Reflect;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Note: World.node methods are tested in NodeTest. */
public class WorldTest {
    @Test
    public void close() throws IOException {
        FileNode tmp;

        try (World world = World.createMinimal()) {
            tmp = world.getTemp().createTempFile();
            assertTrue(tmp.isFile());
        }
        assertFalse(tmp.isFile());
        try (World world = World.createMinimal()) {
            tmp = world.getTemp().createTempFile();
            assertTrue(tmp.isFile());
        }
        assertFalse(tmp.isFile());
    }

    @Test
    public void getFilesystem() {
        World world;

        world = World.createMinimal();
        assertTrue(world.getFilesystem("file") instanceof FileFilesystem);
    }

    @Test(expected=IllegalArgumentException.class)
    public void filesystemDuplicate() {
        World world;

        world = World.createMinimal();
        world.addFilesystem(new FileFilesystem(world, "file"));
    }

    @Test
    public void filesystemParse() throws Exception {
        Node node;
        World world;

        world = World.create();
        node = world.node(new File(File.listRoots()[0], "usr").toURI());
        assertEquals("usr", node.getPath());
        node = world.node("console:///");
        assertTrue(node instanceof ConsoleNode);
        node = world.node("mem://1/foo");
        assertTrue(node instanceof MemoryNode);
    }

    @Test
    public void nodeForUri() throws IOException, URISyntaxException {
        URI uri;
        Node node;
        World world;

        world = World.create();
        uri = new URI("http://foo.bar:80/foo");
        node = world.node(uri);
        assertTrue(node instanceof HttpNode);
        assertEquals("foo", node.getPath());
        assertEquals(uri, node.getUri());

        uri = new URI("file:/home/mhm/bar.txt");
        node = world.node(uri);
        assertTrue(node instanceof FileNode);
        assertEquals("home/mhm/bar.txt", node.getPath());

        uri = getClass().getClassLoader().getResource("org/junit/Test.class").toURI();
        node = world.node(uri);
        assertTrue(node instanceof ZipNode);
        assertEquals("org/junit/Test.class", node.getPath());
    }

    //--

    @Test
    public void path() throws Exception {
        World world;
        List<FileNode> path;

        world = World.createMinimal();
        assertEquals(0, world.path("").size());
        path = world.path("foo" + world.os.listSeparator.getSeparator() + "bar");
        assertEquals(2, path.size());
        assertEquals("foo", path.get(0).toString());
        assertEquals("bar", path.get(1).toString());
    }

    @Test
    public void uripath() throws Exception {
        World world;
        List<FileNode> path;

        world = World.createMinimal();
        assertEquals(0, world.path("").size());
        path = world.path(new File("foo").getAbsolutePath() + world.os.listSeparator.getSeparator() + new File("bar").getAbsolutePath());
        assertEquals(2, path.size());
        assertEquals("foo", path.get(0).getName());
        assertEquals("bar", path.get(1).getName());
    }

    //-- resources

    @Test
    public void fileResource() throws Exception {
        Node node;

        node = World.createMinimal().resource("testresource");
        assertTrue(node instanceof FileNode);
        assertTrue(node.isFile());
        assertEquals("hello", node.readString());
    }

    @Test
    public void zipFileResource() throws Exception {
        Node node;

        node = World.create().resource("org/junit/Assert.class");
        assertTrue(node instanceof ZipNode);
        assertTrue(node.isFile());
        assertEquals(node.size(), node.readBytes().length);
    }

    @Test
    public void zipDirectoryResource() throws Exception {
        Node node;

        node = World.create(false).resource("org/junit");
        assertTrue(node instanceof ZipNode);
        assertTrue(node.isDirectory());
        assertTrue(node.list().size() > 0);
    }


    @Test
    public void resourcesDirectory() throws IOException {
        World world;
        List<Node<?>> lst;

        world = World.create();
        lst = world.resources("org");
        System.out.println("" + lst);
    }

    @Test(expected=FileNotFoundException.class)
    public void noneExisting() throws Exception {
        World.createMinimal().resource("nosuchresource");
    }

    @Test(expected=IllegalArgumentException.class)
    public void absolutePath() throws Exception {
        World.createMinimal().resource("/absolute");
    }

    //-- locating

    @Test
    public void locateClasspathEntry() throws IOException {
        World world;

        world = World.createMinimal();
        world.locateClasspathEntry(World.class).checkDirectory(); // target/classes
        world.locateClasspathEntry(Reflect.resourceName(WorldTest.class)).checkDirectory();
        world.locateClasspathEntry(Test.class).checkFile(); // locate a dependency
        assertEquals("foo bar.jar", world.locateClasspathEntry(new URL("jar:file:/foo%20bar.jar!/some/file.txt"), "/some/file.txt").getPath());
        assertEquals("foo+bar.jar", world.locateClasspathEntry(new URL("jar:file:/foo+bar.jar!/some/file.txt"), "/some/file.txt").getPath());

        if (System.getProperty("java.version").startsWith("1.")) {
            world.locateClasspathEntry(Object.class).checkFile();
        } else {
            try {
                world.locateClasspathEntry(Object.class).checkFile();
                fail();
            } catch (RuntimeException e) {
                // ok
            }
        }
    }


    @Test(expected=RuntimeException.class)
    public void locateClasspathEntryNotFound() {
        World world;

        world = World.createMinimal();
        world.locateClasspathEntry("/nosuchresource");
    }

    @Test
    public void locateFromJar() throws IOException {
        World world;

        world = World.createMinimal();
        world.locateClasspathEntry("/org/junit/Test.class").checkExists();
    }

    @Test
    public void projectHome() {
        check(World.class);
        check(WorldTest.class);
    }

    private void check(Class<?> clazz) {
        World world;
        FileNode home;

        world = World.createMinimal();
        home = world.guessProjectHome(clazz);
        assertNotNull(home);
        assertTrue(home.join("pom.xml").isFile());
    }
}
