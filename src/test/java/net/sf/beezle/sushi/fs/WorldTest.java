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

package net.sf.beezle.sushi.fs;

import net.sf.beezle.sushi.fs.console.ConsoleNode;
import net.sf.beezle.sushi.fs.file.FileFilesystem;
import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.fs.memory.MemoryNode;
import net.sf.beezle.sushi.fs.webdav.WebdavNode;
import net.sf.beezle.sushi.fs.zip.ZipFilesystem;
import net.sf.beezle.sushi.fs.zip.ZipNode;
import net.sf.beezle.sushi.util.Reflect;
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
        assertEquals("home/mhm/bar.txt".replace('/', node.getRoot().getFilesystem().getSeparatorChar()), 
        		node.getPath());

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
        path = world.path("foo" + world.os.listSeparator + "bar");
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
        path = world.path(new File("foo").getAbsolutePath() + world.os.listSeparator + new File("bar").getAbsolutePath());
        assertEquals(2, path.size());
        assertEquals("foo", path.get(0).getName());
        assertEquals("bar", path.get(1).getName());
    }

    //--

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

    //--

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


    //--

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
