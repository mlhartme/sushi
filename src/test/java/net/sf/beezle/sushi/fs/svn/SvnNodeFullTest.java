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

package net.sf.beezle.sushi.fs.svn;

import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.NodeInstantiationException;
import net.sf.beezle.sushi.fs.NodeTest;
import net.sf.beezle.sushi.fs.file.FileNode;
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
        URL = SVNRepositoryFactory.createLocalRepository(repo.getFile(), null, true, true, true);
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
        FileNode ws;
        SvnNode svn;

        ws = WORLD.guessProjectHome(getClass());
        svn = SvnNode.fromWorkspace(ws);
        svn.checkDirectory();
        svn = SvnNode.fromWorkspace(ws.join("pom.xml"));
        svn.checkFile();
    }

    //--

    private SvnNode create(String path) throws NodeInstantiationException {
        return (SvnNode) WORLD.validNode("svn:" + path);
    }
}
