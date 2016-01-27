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
package net.oneandone.sushi.fs.file;

import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeTest;
import net.oneandone.sushi.io.OS;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** TODO: move more tests into NodeTest */
public class FileNodeTest extends NodeTest<FileNode> {
    @Override
    protected FileNode createWork() throws IOException {
        return WORLD.getTemp().createTempDirectory();
    }

    @Test(expected = NoSuchFileException.class)
    public void createTempFileInNonExisting() throws IOException {
        work.join("nosuchdir").createTempFile();
    }

    @Test(expected = NoSuchFileException.class)
    public void createTempDirectoryInNonExisting() throws IOException {
        work.join("nosuchdir").createTempDirectory();
    }

    @Test
    public void toStr() {
        FileNode orig;
        Node node;

        orig = work.getWorld().getWorking();
        try {
            work.getWorld().setWorking(work);
            node = work.join("foo");
            assertEquals(".", work.toString());
            assertEquals("foo", node.toString());
            work.getWorld().setWorking(null);
            assertEquals(((FileNode) node).toPath().toString(), node.toString());
        } finally {
            work.getWorld().setWorking(orig);
        }
    }

    @Test
    public void fileConstructor() {
        assertEquals(WORLD.getHome(), WORLD.file(System.getProperty("user.home")));
        assertEquals(work.getPath(), WORLD.file(work.getAbsolute() + "/").getPath());
        assertEquals(work.getPath(), WORLD.file(new File(work.getAbsolute() + "/")).getPath());
        assertEquals("", WORLD.file(File.listRoots()[0]).getPath());
    }

    @Override
    public void validateDeallocation() {
        // nothing to check
    }

    @Test
    public void relativeFile() throws IOException {
        FileNode file;

        assertEquals((WORLD.getWorking()).toPath(), new File(".").getCanonicalFile().toPath());
        file = WORLD.file("foo");
        assertEquals("foo", file.toString());
        assertEquals(WORLD.getWorking(), file.getParent());
    }

    // TODO: move up to NodeTest
    @Test
    public void moveFile() throws IOException {
        FileNode src;
        FileNode dest;

        src = work.join("src");
        dest = work.join("dest");
        try {
            src.move(dest);
            fail();
        } catch (FileNotFoundException e) {
            // ok
        }
        src.mkfile();
        src.move(dest);
        assertFalse(src.exists());
        assertTrue(dest.exists());
        src.mkfile();
        try {
            dest.move(src);
            fail();
        } catch (IOException e) {
            // ok
        }
    }

    @Test
    public void mkfile() throws IOException {
        FileNode file;

        file = work.join("mkfile");
        assertFalse(file.exists());
        file.mkfile();
        assertTrue(file.exists());
        assertTrue(file.isFile());
        try {
            file.mkfile();
            fail();
        } catch (IOException e) {
            // ok
        }
        try {
            file.mkdir();
            fail();
        } catch (IOException e) {
            // ok
        }
        file.deleteTree();
    }

    @Test
    public void linkToProtectedDirectory() throws IOException {
        if (OS.CURRENT == OS.WINDOWS) {
            return; // TODO: is it save to delete root?
        }

        FileNode link;

        link = work.join("link");
        work.getRootNode().link(link);
        link.deleteTree();
    }

    @Test
    public void linkToProtectedFile() throws IOException {
        if (OS.CURRENT == OS.WINDOWS) {
            return; // TODO: is it save to delete root?
        }

        FileNode link;

        link = work.join("link");
        work.getRootNode().join("etc/passwd").link(link);
        link.deleteTree();
    }

    @Test
    public void brokenLink() throws IOException {
        FileNode link;

        link = work.join("link");
        link.mklink("nosuchfile");
        assertTrue(link.exists());
        try {
            link.deleteFile();
            fail();
        } catch (FileNotFoundException e) {
            // ok
        }
        assertTrue(link.exists());
        link.deleteTree();
        assertFalse(link.exists());
    }

    @Test
    public void modeFile() throws IOException {
        checkMode(WORLD.getTemp().createTempFile());
    }

    @Test
    public void modeDir() throws IOException {
        checkMode(WORLD.getTemp().createTempDirectory());
    }

    private void checkMode(FileNode node) throws IOException {
        if (node.getWorld().os == OS.WINDOWS) {
            return; // TODO
        }
        checkMode(node, "rw-r--r--");
        checkMode(node, "rwx------");
        assertTrue(node.canRead());
        assertTrue(node.canWrite());
        checkMode(node, "---------");
        assertFalse(node.canRead());
        assertFalse(node.canWrite());
        checkMode(node, "rwxrwxrwx");
        assertTrue(node.canRead());
        assertTrue(node.canWrite());
    }

    private void checkMode(FileNode node, String permissions) throws IOException {
        node.setPermissions(permissions);
        assertEquals(permissions, node.getPermissions());
    }

    //--

    @Test
    public void temp() throws IOException {
        FileNode tmp;

        tmp = work.createTempFile();
        assertEquals("", tmp.readString());
        tmp = work.createTempDirectory();
        assertEquals(0, tmp.list().size());
    }
}

