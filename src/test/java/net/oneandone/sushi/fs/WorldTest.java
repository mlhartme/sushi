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
package net.oneandone.sushi.fs;

import net.oneandone.sushi.fs.console.ConsoleNode;
import net.oneandone.sushi.fs.file.FileFilesystem;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.fs.memory.MemoryNode;
import net.oneandone.sushi.fs.webdav.WebdavNode;
import net.oneandone.sushi.fs.zip.ZipFilesystem;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Note: World.node methods are tested in NodeTest. */
public class WorldTest {

    //-- filesystems

    @Test
    public void getFilesystem() {
        World world;

        world = new World();
        assertTrue(world.getFilesystem("zip") instanceof ZipFilesystem);
    }

    @Test(expected=IllegalArgumentException.class)
    public void filesystemDuplicate() {
        World world;

        world = new World();
        world.addFilesystem(new FileFilesystem(world, "file"));
    }

    @Test
    public void filesystemParse() throws Exception {
        Node node;
        World world;

        world = new World();
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

        world = new World();
        uri = new URI("http://foo.bar:80/foo");
        node = world.node(uri);
        assertTrue(node instanceof WebdavNode);
        assertEquals("foo", node.getPath());
        assertEquals(uri, node.getURI());

        uri = new URI("file:/home/mhm/bar.txt");
        node = world.node(uri);
        assertTrue(node instanceof FileNode);
        assertEquals("home/mhm/bar.txt", node.getPath());

        uri = getClass().getClassLoader().getResource("java/lang/Object.class").toURI();
        node = world.node(uri);
        assertTrue(node instanceof ZipNode);
        assertEquals("java/lang/Object.class", node.getPath());
    }

    //--

    @Test
    public void path() throws Exception {
        World world;
        List<FileNode> path;

        world = new World();
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

        world = new World();
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

        node = new World().resource("testresource");
        assertTrue(node instanceof FileNode);
        assertTrue(node.isFile());
        assertEquals("hello", node.readString());
    }

    @Test
    public void zipResource() throws Exception {
        Node node;

        node = new World().resource("org/junit/Assert.class");
        assertTrue(node instanceof ZipNode);
        assertTrue(node.isFile());
        assertEquals(node.length(), node.readBytes().length);
    }

    @Test(expected=FileNotFoundException.class)
    public void noneExisting() throws Exception {
        new World().resource("nosuchresource");
    }

    @Test(expected=IllegalArgumentException.class)
    public void absolutePath() throws Exception {
        new World().resource("/absolute");
    }


    //-- locating

    @Test(expected=RuntimeException.class)
    public void locate() throws IOException {
        World world;

        world = new World();
        world.locateClasspathItem(World.class).checkDirectory();
        world.locateClasspathItem(Reflect.resourceName(WorldTest.class)).checkDirectory();
        world.locateClasspathItem(Object.class).checkFile();
        world.locateClasspathItem("/java/lang/Object.class").checkFile();
        world.locateClasspathItem("/java/lang/Object.class").checkFile();
        assertEquals("foo bar.jar", world.locateClasspathItem(new URL("jar:file:/foo%20bar.jar!/some/file.txt"), "/some/file.txt").getPath());
        assertEquals("foo+bar.jar", world.locateClasspathItem(new URL("jar:file:/foo+bar.jar!/some/file.txt"), "/some/file.txt").getPath());
        world.locateClasspathItem("/nosuchresource");
    }

    @Test
    public void locateRuntime() throws IOException {
        World world;

        world = new World();
        world.locateClasspathItem("/java/lang/Object.class").checkExists();
    }

    @Test
    public void locateFromJar() throws IOException {
        World world;

        world = new World();
        world.locateClasspathItem("/org/junit/Test.class").checkExists();
    }

    @Test
    public void projectHome() {
        check(World.class);
        check(WorldTest.class);
    }

    private void check(Class<?> clazz) {
        World world;
        FileNode home;

        world = new World();
        home = world.guessProjectHome(clazz);
        assertNotNull(home);
        assertTrue(home.join("pom.xml").isFile());
    }
}
