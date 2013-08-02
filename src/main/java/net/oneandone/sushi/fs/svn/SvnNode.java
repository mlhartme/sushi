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

import com.jcraft.jsch.JSchException;
import net.oneandone.sushi.fs.*;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.CheckedByteArrayOutputStream;
import net.oneandone.sushi.io.SkipOutputStream;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNFileRevision;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class SvnNode extends Node {
    private final SvnRoot root;
    private final String path;

    public SvnNode(SvnRoot root, String path) {
        super();
        this.root = root;
        this.path = path;
    }

    @Override
    public URI getURI() {
        return URI.create(root.getFilesystem().getScheme() + ":" + getSvnurl().toString());
    }

    @Override
    public SvnRoot getRoot() {
        return root;
    }

    @Override
    public SvnNode getParent() {
        return (SvnNode) doGetParent();
    }

    @Override
    public SvnNode join(String ... paths) {
        return (SvnNode) doJoin(paths);
    }

    @Override
    public SvnNode join(List<String> paths) {
        return (SvnNode) doJoin(paths);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public List<SvnNode> list() throws DirectoryNotFoundException, ListException {
        List<SVNDirEntry> lst;
        List<SvnNode> result;
        SVNRepository repository;
        SvnNode child;
        SVNNodeKind kind;

        repository = root.getRepository();
        try {
            kind = repository.checkPath(path, -1);
            if (kind == SVNNodeKind.DIR) {
                lst = new ArrayList<>();
                repository.getDir(path, -1, false, lst);
                result = new ArrayList<>(lst.size());
                for (SVNDirEntry entry : lst) {
                    child = new SvnNode(root, doJoin(path, entry.getRelativePath()));
                    result.add(child);
                }
                return result;
            } else if (kind == SVNNodeKind.FILE) {
                return null;
            } else {
                throw new DirectoryNotFoundException(this);
            }
        } catch (SVNException e) {
            throw new ListException(this, e);
        }
    }

    public SVNDirEntry info() throws SVNException {
        return root.getRepository().info(path, SVNRevision.HEAD.getNumber());
    }

    public long getLatestRevision() throws SVNException {
        List<Long> revs;
        SVNDirEntry dir;

        if (root.getRepository().checkPath(path, -1) == SVNNodeKind.DIR) {
            dir = root.getRepository().getDir(path, -1, false, new ArrayList<>());
            return dir.getRevision();
        } else {
            revs = getRevisions();
            return revs.get(revs.size() - 1);
        }
    }

    public List<Long> getRevisions() throws SVNException {
        return getRevisions(0);
    }

    public List<Long> getRevisions(long start) throws SVNException {
        return getRevisions(start, root.getRepository().getLatestRevision());
    }

    public List<Long> getRevisions(long start, long end) throws SVNException {
        Collection<SVNFileRevision> revisions;
        List<Long> result;

        revisions = (Collection<SVNFileRevision>) root.getRepository().getFileRevisions(path, null, start, end);
        result = new ArrayList<>();
        for (SVNFileRevision rev : revisions) {
            result.add(rev.getRevision());
        }
        return result;
    }

    @Override
    public String getPermissions() {
        throw unsupported("getPermissions()");
    }

    @Override
    public void setPermissions(String permissions) {
        throw unsupported("setPermissions()");
    }

    @Override
    public UserPrincipal getOwner() {
        throw unsupported("getOwner()");
    }

    @Override
    public void setOwner(UserPrincipal owner) {
        throw unsupported("setOwner()");
    }

    @Override
    public GroupPrincipal getGroup() {
        throw unsupported("getGroup()");
    }

    @Override
    public void setGroup(GroupPrincipal group) {
        throw unsupported("setGroup()");
    }

    @Override
    public InputStream createInputStream() throws CreateInputStreamException, FileNotFoundException {
        FileNode tmp;

        try {
            tmp = getWorld().getTemp().createTempFile();
            try (OutputStream dest = tmp.createOutputStream()) {
                writeTo(dest);
            }
            return tmp.createInputStream();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new CreateInputStreamException(this, e);
        }
    }

    public long writeTo(OutputStream dest, long skip) throws WriteToException, FileNotFoundException {
        SkipOutputStream out;

        out = new SkipOutputStream(dest, skip);
        try {
            doWriteTo(-1, out);
        } catch (SVNException e) {
            throw new WriteToException(this, e);
        }
        return out.count();
    }


    @Override
    public OutputStream createOutputStream(boolean append) throws CreateOutputStreamException, FileNotFoundException {
        byte[] add;

        try {
            if (isDirectory()) {
                throw new FileNotFoundException(this);
            }
        } catch (ExistsException e) {
            throw new CreateOutputStreamException(this, e);
        }
        try {
            if (append) {
                try {
                    add = readBytes();
                } catch (FileNotFoundException e) {
                    add = null;
                }
            } else {
                add = null;
            }
            return new CheckedByteArrayOutputStream(add) {
                @Override
                public void close() throws IOException {
                    super.close();
                    try {
                        readFrom(new ByteArrayInputStream(toByteArray()), root.getComment());
                    } catch (SVNException e) {
                        throw new IOException("close failed", e);
                    }
                }
            };
        } catch (IOException e) {
            throw new CreateOutputStreamException(this, e);
        }
    }

    @Override
    public SvnNode deleteFile() throws FileNotFoundException, DeleteException {
        try {
            if (!isFile()) {
                throw new FileNotFoundException(this);
            }
            delete("sushi delete");
        } catch (ExistsException | SVNException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public SvnNode deleteDirectory() throws DeleteException, DirectoryNotFoundException {
        List<SvnNode> lst;

        try {
            lst = list();
            if (lst == null) {
                throw new DirectoryNotFoundException(this);
            }
            if (lst.size() > 0) {
                throw new DeleteException(this, "directory is not empty");
            }
            delete("sushi delete");
        } catch (ListException | SVNException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public SvnNode deleteTree() throws NodeNotFoundException, DeleteException {
        try {
            if (!exists()) {
                throw new NodeNotFoundException(this);
            }
            delete("sushi delete");
        } catch (ExistsException | SVNException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    /** @return revision */
    public long delete(String comment) throws SVNException {
        SVNCommitClient client;
        SVNCommitInfo info;

        client = root.getClientMananger().getCommitClient();
        info = client.doDelete(new SVNURL[] { getSvnurl() }, comment);
        return info.getNewRevision();
    }

    @Override
    public Node mkdir() throws MkdirException {
        SVNCommitClient client;

        try {
            client = root.getClientMananger().getCommitClient();
            client.doMkDir(new SVNURL[] { getSvnurl() }, root.getComment());
            return this;
        } catch (SVNException e) {
            throw new MkdirException(this, e);
        }
    }

    @Override
    public void mklink(String target) {
        throw unsupported("mklink()");
    }

    @Override
    public String readLink() {
        throw unsupported("readLink()");
    }

    /** @return revision */
    public long doWriteTo(long revision, OutputStream dest) throws FileNotFoundException, SVNException {
        SVNRepository repository;

        repository = root.getRepository();
        if (repository.checkPath(path, revision) != SVNNodeKind.FILE) {
            throw new FileNotFoundException(this, "no such file in revision " + revision);
        }
        return repository.getFile(path, revision, null, dest);
    }

    @Override
    public boolean exists() throws ExistsException {
        try {
            return exists(root.getRepository().getLatestRevision());
        } catch (SVNException e) {
            throw new ExistsException(this, e);
        }
    }

    public boolean exists(long revision) throws SVNException {
        SVNNodeKind kind;

        kind = root.getRepository().checkPath(path, revision);
        return kind == SVNNodeKind.FILE || kind == SVNNodeKind.DIR;
    }

    @Override
    public long length() throws LengthException {
        SVNDirEntry entry;
        try {
            entry = root.getRepository().info(path, -1);
            if (entry == null || entry.getKind() != SVNNodeKind.FILE) {
                throw new LengthException(this, new IOException("file expected"));
            }
            return entry.getSize();
        } catch (SVNException e) {
            throw new LengthException(this, e);
        }
    }

    @Override
    public boolean isFile() throws ExistsException {
        return kind() == SVNNodeKind.FILE;
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        return kind() == SVNNodeKind.DIR;
    }

    @Override
    public boolean isLink() {
    	return false;
    }

    private SVNNodeKind kind() throws ExistsException {
        SVNRepository repository;

        repository = root.getRepository();
        try {
            return repository.checkPath(path, repository.getLatestRevision());
        } catch (SVNException e) {
            throw new ExistsException(this, e);
        }
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        return getLastModified(-1).getTime();
    }

    public Date getLastModified(long revision) throws GetLastModifiedException {
        try {
            if (!exists()) {
                throw new GetLastModifiedException(this, null);
            }
        } catch (ExistsException e) {
            throw new GetLastModifiedException(this, e);
        }
        try {
            return root.getRepository().info(path, revision).getDate();
        } catch (SVNException e) {
            throw new GetLastModifiedException(this, e);
        }
    }

    @Override
    public void setLastModified(long millis) throws SetLastModifiedException {
        throw new SetLastModifiedException(this);
    }

    /** @return revision */
    public long readFrom(InputStream content, String comment) throws SVNException {
    	// does NOT use the CommitClient, because the commit client needs a physical file
        boolean exists;
        ISVNEditor editor;
        SVNCommitInfo info;
        SVNDeltaGenerator deltaGenerator;
        String checksum;
        SVNRepository repository;

        repository = root.getRepository();
        try {
            exists = exists();
        } catch (ExistsException e) {
            throw (SVNException) e.getCause();
        }
        editor = repository.getCommitEditor(comment, null);
        editor.openRoot(-1);
        editor.openDir(SVNPathUtil.removeTail(path), -1);
        if (exists) {
            editor.openFile(path, -1);
        } else {
            editor.addFile(path, null, -1);
        }
        editor.applyTextDelta(path, null);
        deltaGenerator = new SVNDeltaGenerator();
        checksum = deltaGenerator.sendDelta(path, content, editor, true);
        editor.closeFile(path, checksum);
        editor.closeDir();
        info = editor.closeEdit();
        return info.getNewRevision();
    }

    //--

    // TODO
    private String doJoin(String left, String right) {
        if (left.length() == 0) {
            return right;
        }
        return left + Filesystem.SEPARATOR_STRING + right;
    }

    public long export(Node dest) throws IOException, SVNException {
        long latest;

        latest = getLatestRevision();
        export(dest, latest);
        return latest;
    }

    public void export(Node dest, long revision) throws IOException, SVNException {
        Exporter exporter;
        SVNRepository sub;
        SVNRepository repository;

        repository = root.getRepository();
        this.checkDirectory();
        dest.checkDirectory();
        exporter = new Exporter(revision, dest);
        if (path.length() == 0) {
            sub = repository;
        } else {
            // repository updates has a target to restrict the result, but it supports
            // only one segment. So I have to create a new repository ...
            sub = SvnFilesystem.repository(getSvnurl(), root.getRepository().getAuthenticationManager());
        }
        sub.update(revision, "", true, exporter, exporter);
    }

    public long checkout(FileNode dest) throws IOException, SVNException {
        long latest;

        latest = getLatestRevision();
        checkout(dest, latest);
        return latest;
    }

    public void checkout(FileNode dest, long revision) throws IOException, SVNException {
        checkDirectory();
        dest.checkDirectory();
        SVNUpdateClient client = getRoot().getClientMananger().getUpdateClient();
        client.doCheckout(getSvnurl(), dest.toPath().toFile(), SVNRevision.UNDEFINED, SVNRevision.create(revision),
                SVNDepth.INFINITY, false);
    }

    public SVNURL getSvnurl() {
        try {
            return root.getRepository().getLocation().appendPath(path, false);
        } catch (SVNException e) {
            throw new IllegalStateException(path, e);
        }
    }

    /** @param workspace a file or directory */
    public static SvnNode fromWorkspace(FileNode workspace) throws IOException {
        FileNode dir;
        SvnNode result;

        dir = workspace.isFile() ? workspace.getParent() : workspace;
        result = (SvnNode) workspace.getWorld().validNode("svn:" + urlFromWorkspace(dir));
        if (dir != workspace) {
            result = result.join(workspace.getName());
        }
        return result;
    }

    public static String urlFromWorkspace(FileNode workspace) throws IOException {
        SVNClientManager clientManager;

        clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true));
        try {
            return clientManager.getWCClient().doInfo(workspace.toPath().toFile(), SVNRevision.UNDEFINED).getURL().toString();
        } catch (SVNException e) {
            throw new IOException("cannot determine workspace url: " + e.getMessage(), e);
        }
    }
}
