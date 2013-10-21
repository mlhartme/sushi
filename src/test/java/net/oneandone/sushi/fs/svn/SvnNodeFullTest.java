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
package net.oneandone.sushi.fs.svn;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.NodeTest;
import net.oneandone.sushi.fs.file.FileNode;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SvnNodeFullTest extends NodeTest<SvnNode> {
    private static SVNURL URL;

    @BeforeClass
    public static void setUpClass() throws Exception {
        FileNode repo;

        repo = WORLD.guessProjectHome(SvnNodeFullTest.class).join("target/svnrepo");
        repo.deleteTreeOpt();
        URL = SVNRepositoryFactory.createLocalRepository(repo.toPath().toFile(), null, true, true, true);
    }

    @Override
    public SvnNode createWork() throws IOException {
        SvnNode node;

        node = create(URL + "/work");
        node.deleteTreeOpt();
        node.mkdir();
        return node;
    }

    @Override
    public void validateDeallocation() {
        // nothing to check
    }

    @Test
    public void rootLocator() {
        assertEquals("svn:" + URL.toString() + "/work", work.getURI().toString());
    }


    @Test
    public void rootWithUrl() {
        assertEquals(URL.toString() + "/", work.getRoot().getId());
    }

    @Test
    public void path() throws IOException {
        assertEquals("", create(URL.toString()).getPath());
        // assertEquals("test", SvnNode.create(WORLD, TEST).getPath());
        assertEquals("work", work.getPath());
    }

    @Test(expected= NodeInstantiationException.class)
    public void connectionRefused() throws IOException {
        create("https://heise.de/svn");
    }

    @Test
    public void revisions() throws IOException, SVNException {
        SvnNode root;
        long rootRevision;
        long fileRevision;

        root = create(URL.toString());
        rootRevision = root.getLatestRevision();
        fileRevision = root.join("work").getLatestRevision();
        assertTrue(fileRevision <= rootRevision);
    }

    @Test
    public void find() throws IOException {
        SvnNode root;
        List<Node> lst;

        root = create(URL.toString());
        lst = WORLD.filter().include("*").collect(root);
        assertEquals(1, lst.size());
    }

    @Test
    public void export() throws IOException, SVNException {
        SvnNode root;
        FileNode dir;

        root = work;
        root.join("file").writeString("foo");
        root.join("dir").mkdir().join("sub").writeString("bar");
        root.join("dir/dir1/dir2").mkdirs();
        root.join("dir/dir1/dir2").join("sub1").writeString("baz");

        dir = work.getWorld().getTemp().createTempDirectory();
        root.export(dir);
        assertEquals("foo", dir.join("file").readString());
        assertEquals("bar", dir.join("dir/sub").readString());
        assertEquals("baz", dir.join("dir/dir1/dir2/sub1").readString());

        dir = work.getWorld().getTemp().createTempDirectory();
        root.join("dir").export(dir);
        assertEquals("bar", dir.join("sub").readString());

        dir = work.getWorld().getTemp().createTempDirectory();
        root.join("dir/dir1/dir2").export(dir);
        assertEquals("baz", dir.join("sub1").readString());
    }

    @Test
    public void svnurl() {
        assertEquals(URL + "/work/a", work.join("a").getSvnurl().toString());
    }

    @Test
    public void fromWorkspace() throws Exception {
        SvnNode orig;
        FileNode dest;
        SvnNode svn;

        orig = (SvnNode) WORLD.node("svn:https://svn.code.sf.net/p/packlet/code/kinderhaus/site");
        dest = WORLD.getTemp().createTempDirectory();
        orig.checkout(dest);
        svn = SvnNode.fromWorkspace(dest);
        svn.checkDirectory();
        assertEquals(svn, orig);
    }

    //--

    private SvnNode create(String path) throws NodeInstantiationException {
        return (SvnNode) WORLD.validNode("svn:" + path);
    }
}
