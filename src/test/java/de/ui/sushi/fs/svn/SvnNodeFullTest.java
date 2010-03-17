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

package de.ui.sushi.fs.svn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.LocatorException;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.NodeTest;
import de.ui.sushi.fs.RootPathException;
import de.ui.sushi.fs.file.FileNode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

public class SvnNodeFullTest extends NodeTest {
    private static SVNURL URL;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Node repo;

        repo = IO.guessProjectHome(SvnNodeFullTest.class).join("target/svnrepo");
        repo.deleteOpt();
        URL = SVNRepositoryFactory.createLocalRepository(new File(repo.getAbsolute()), null, true, true, true);
    }

    @Override
    public Node createWork() throws IOException {
        SvnNode node;

        node = create(URL + "/work");
        node.deleteOpt();
        node.mkdir();
        return node;
    }

    @Test
    public void rootLocator() throws SVNException {
        assertEquals("svn:" + URL.toString() + "/work", work.getLocator());
    }


    @Test
    public void rootWithUrl() throws SVNException {
        assertEquals(URL.toString() + "/", work.getRoot().getId());
    }

    @Test
    public void path() throws IOException {
        assertEquals("", create(URL.toString()).getPath());
        // assertEquals("test", SvnNode.create(IO, TEST).getPath());
        assertEquals("work", work.getPath());
    }

    @Test(expected=LocatorException.class)
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
        fileRevision = ((SvnNode) root.join("work")).getLatestRevision();
        assertTrue(fileRevision <= rootRevision);
    }

    @Test
    public void find() throws SVNException, IOException {
        SvnNode root;
        List<Node> lst;

        root = create(URL.toString());
        lst = IO.filter().include("*").collect(root);
        assertEquals(1, lst.size());
    }

    @Test
    public void log() throws Exception {
        final String comment = "my comment";
        SvnNode root;
        long revision;

        root = (SvnNode) work;
        revision = ((SvnNode) root.join("file")).save("welcome".getBytes(), comment);
        assertTrue(root.changelog(revision, "viewsvn").contains(comment));
    }

    @Test
    public void export() throws IOException, SVNException {
        SvnNode root;
        FileNode dir;

        root = (SvnNode) work;
        root.join("file").writeString("foo");
        root.join("dir").mkdir().join("sub").writeString("bar");
        root.join("dir/dir1/dir2").mkdirs();
        root.join("dir/dir1/dir2").join("sub1").writeString("baz");

        dir = work.getIO().getTemp().createTempDirectory();
        root.export(dir);
        assertEquals("foo", dir.join("file").readString());
        assertEquals("bar", dir.join("dir/sub").readString());
        assertEquals("baz", dir.join("dir/dir1/dir2/sub1").readString());

        dir = work.getIO().getTemp().createTempDirectory();
        ((SvnNode) root.join("dir")).export(dir);
        assertEquals("bar", dir.join("sub").readString());

        dir = work.getIO().getTemp().createTempDirectory();
        ((SvnNode) root.join("dir/dir1/dir2")).export(dir);
        assertEquals("baz", dir.join("sub1").readString());
    }

    @Test
    public void svnurl() throws SVNException {
        assertEquals(URL + "/work/a", ((SvnNode) work.join("a")).getSvnurl().toString());
    }

    //--

    private SvnNode create(String path) throws RootPathException {
        return (SvnNode) IO.node("svn:" + path);
    }
}
