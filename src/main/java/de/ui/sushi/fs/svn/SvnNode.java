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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import de.ui.sushi.fs.DeleteException;
import de.ui.sushi.fs.ExistsException;
import de.ui.sushi.fs.GetLastModifiedException;
import de.ui.sushi.fs.LengthException;
import de.ui.sushi.fs.ListException;
import de.ui.sushi.fs.LocatorException;
import de.ui.sushi.fs.MkdirException;
import de.ui.sushi.fs.MoveException;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.SetLastModifiedException;
import de.ui.sushi.fs.file.FileNode;
import de.ui.sushi.io.CheckedByteArrayOutputStream;
import de.ui.sushi.util.Program;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNFileRevision;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnNode extends Node {
    private final SvnRoot root;
    private final boolean directory;
    private final String path;
    
    public SvnNode(SvnRoot root, boolean directory, String path) {
        super();
        this.root = root;
        this.directory = directory;
        this.path = path;
    }

    @Override
    public SvnRoot getRoot() {
        return root;
    }
    
    @Override
    public String getPath() {
        return path;
    }
    
    @Override
    public List<SvnNode> list() throws ListException {
        List<SVNDirEntry> lst;
        List<SvnNode> result;
        SVNDirEntry entry;
        SVNRepository repository;
        SvnNode child;
        
        repository = root.getRepository();
        try {
            if (repository.checkPath(path, -1) != SVNNodeKind.DIR) {
                return null;
            }
            lst = new ArrayList<SVNDirEntry>();
            repository.getDir(path, -1, false, lst);
            result = new ArrayList<SvnNode>(lst.size());
            for (int i = 0; i < lst.size(); i++) {
                entry = lst.get(i);
                child = new SvnNode(root, entry.getKind() == SVNNodeKind.DIR, doJoin(path, entry.getRelativePath()));
                child.setBase(getBase());
                result.add(child);
            }
            return result;
        } catch (SVNException e) {
            throw new ListException(this, e);
        }
    }
    
    public long getLatestRevision() throws SVNException {
        List<Long> revs;
        SVNDirEntry dir;
        
        if (directory) {
            dir = root.getRepository().getDir(path, -1, false, new ArrayList<Object>());
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
        result = new ArrayList<Long>();
        for (SVNFileRevision rev : revisions) {
            result.add(rev.getRevision());
        }
        return result;
    }

    @Override 
    public int getMode() {
        throw unsupported("getMode()");
    }

    @Override
    public void setMode(int mode) {
        throw unsupported("setMode()");
    }
    
    @Override 
    public int getUid() {
        throw unsupported("getUid()");
    }

    @Override
    public void setUid(int uid) {
        throw unsupported("setUid()");
    }

    @Override 
    public int getGid() {
        throw unsupported("getGid()");
    }

    @Override
    public void setGid(int gid) {
        throw unsupported("setGid()");
    }

    @Override
    public InputStream createInputStream() throws IOException {
        FileNode tmp;
        OutputStream dest;
        
        tmp = getIO().getTemp().createTempFile();
        dest = tmp.createOutputStream();
        try {
            load(dest);
        } catch (SVNException e) {
            throw new IOException("svn failure", e);
        }
        dest.close();
        return tmp.createInputStream();
    }
    
    @Override
    public OutputStream createOutputStream(boolean append) throws IOException {
    	if (append) {
    		throw unsupported("createOutputStream(true)");
    	}
        return new CheckedByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    save(toByteArray(), root.getComment());
                } catch (SVNException e) {
                    throw new IOException("close failed", e);
                }
            }
        };
    }

    @Override
    public SvnNode delete() throws DeleteException {
        try {
            if (!exists()) {
                throw new DeleteException(this, new FileNotFoundException());
            }
            delete("sushi delete");
        } catch (ExistsException e) {
            throw new DeleteException(this, e);
        } catch (SVNException e) {
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
    public Node move(Node dest) throws MoveException {
    	throw new MoveException(this, dest, "SvnNode cannot be moved");
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
    
    public long load(OutputStream dest) throws SVNException, FileNotFoundException {
        return load(root.getRepository().getLatestRevision(), dest);
    }

    public long load(long revision, OutputStream dest) throws FileNotFoundException, SVNException {
        SVNRepository repository;
        
        repository = root.getRepository();
        if (repository.checkPath(path, revision) != SVNNodeKind.FILE) {
            throw new FileNotFoundException("no such file: " + path + ", revision " + revision);
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
    public long save(byte[] content, String comment) throws SVNException {
        return save(new ByteArrayInputStream(content), comment);
    }
    
    /** @return revision */ 
    public long save(InputStream content, String comment) throws SVNException {
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
        return left + root.getFilesystem().getSeparator() + right;
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
            sub = SvnFilesystem.repository(repository.getLocation().appendPath(path, true), null, null); // TODO: auth
        }
        sub.update(revision, "", true, exporter, exporter);
    }
    
    // --

    /** @param viewvc may be null */
    public String changelog(long startRevision, String viewvc) throws SVNException {
        StringBuilder result;
        
        result = new StringBuilder();
        changelog(startRevision, viewvc, result);
        return result.toString();
    }
    
    /**
     *  @param viewvc may be null
     *  @return revision 
     */
    public long changelog(long startRevision, String viewvc, StringBuilder result) throws SVNException {
        return changelog(startRevision, root.getRepository().getLatestRevision(), false, viewvc, result);
    }

    /**
     * @param viewvc may be null
     * @return revision of last change 
     */
    public long changelog(long startRevision, long endRevision, boolean strictNodes, String viewvc, StringBuilder result) throws SVNException {
        Collection<SVNLogEntry> changeset;
        
        changeset = queryChanges(startRevision, endRevision, strictNodes);
        report(viewvc, changeset, result);
        return getRevision(changeset);
    }

    private long getRevision(Collection<SVNLogEntry> entries) {
        long revision;
        
        revision = 0;
        for (SVNLogEntry logEntry : entries) {
            revision = Math.max(revision, logEntry.getRevision());
        }
        return revision;
    }
    
    private Collection<SVNLogEntry> queryChanges(long startRevision, long endRevision, boolean strictNodes) throws SVNException {
        SVNRepository repository;
        
        repository = root.getRepository();
        if (startRevision > endRevision) {
            // empty log - might happen if "last deployed revision + 1" is passed to this function
            return new ArrayList<SVNLogEntry>();
        }
        return (Collection<SVNLogEntry>) repository.log(new String[] { getPath() }, null, startRevision, endRevision, true, strictNodes);
    }

    /** @param viewvc may be null */
    private void report(String viewvc, Collection<SVNLogEntry> entries, StringBuilder dest) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SVNLogEntryPath entryPath;
        String path;
        
        if (viewvc != null && viewvc.endsWith("/")) {
            throw new IllegalArgumentException(viewvc);
        }
        for (SVNLogEntry entry : entries) {
            dest.append(format.format(entry.getDate())).append(' ').append(entry.getAuthor()).append(' ').
                append(entry.getRevision()).append("\n\n");
            for (String key : (Set<String>) entry.getChangedPaths().keySet()) {
                entryPath = (SVNLogEntryPath) entry.getChangedPaths().get(key);
                path = getPath(entryPath);
                if (path == null) {
                    // file outside this directory that was modified in this change 
                } else {
                    dest.append("  " + entryPath.getType() + " " + toDiff(viewvc, path, entry.getRevision()));
                    dest.append('\n');
                }
            }
            dest.append("\n  ");
            dest.append(entry.getMessage().trim());
            dest.append("\n\n");
        }
    }
    
    private String getPath(SVNLogEntryPath entryPath) {
        String result;
        String path;
        int idx;
        
        result = entryPath.getPath();
        if (!result.startsWith("/")) {
            throw new IllegalArgumentException(result);
        }
        result = result.substring(1);
        path = getPath();
        idx = result.indexOf(path);
        if (idx == -1) {
            return null; // not in this project
        }
        result = result.substring(idx + path.length());
        if (result.equals("")) {
            return ".";
        }
        if (result.startsWith("/")) {
            return result.substring(1);
        }
        return result;
    }
    
    /** @param viewvc may be null */
    private String toDiff(String viewvc, String path, long revision) {
        if (path.startsWith("/")) {
            throw new IllegalArgumentException(path);
        }
        if (viewvc == null) {
            return path;
        } else {
            return path + "\n" + "    " + viewvc + "/" + path + "?r1=" + (revision - 1) + "&r2=" + revision;
        }
    }

    //--
    
    public String checkWorkspace(FileNode basedir, boolean checkRemote) throws SVNException {
        final List<String> local;
        final List<String> remote;
        StringBuilder message;
        SVNClientManager manager;
        SVNRepository repository;
        
        repository = root.getRepository();
        local = new ArrayList<String>();
        remote = new ArrayList<String>();
        manager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), repository.getAuthenticationManager());
        manager.getStatusClient().doStatus(basedir.getFile(), true /* recursive */, checkRemote, 
                    true /* report all */, false /* includeIgnored */, false /* collect parent externals */, new ISVNStatusHandler() {
                        public void handleStatus(SVNStatus status) throws SVNException {
                            SVNStatusType s;

                            s = status.getContentsStatus();
                            if (s != SVNStatusType.STATUS_NONE && s != SVNStatusType.STATUS_NORMAL) {
                                local.add(s.getCode() + " " + status.getFile().getPath());
                            }
                            s = status.getRemoteContentsStatus();
                            if (s != SVNStatusType.STATUS_NONE && s != SVNStatusType.STATUS_NORMAL) {
                                remote.add(s.getCode() + " " + status.getFile().getPath());
                            }
                        }
                    });
        if (local.size() + remote.size() != 0) {
            message = new StringBuilder();
            if (local.size() > 0) {
                message.append("local modifications, please commit you changes:\n");
                for (String file : local) {
                    message.append("  ");
                    message.append(file);
                    message.append('\n');
                }
            }
            if (remote.size() > 0) {
                message.append("remote changes, please update your working copy:\n");
                for (String file : remote) {
                    message.append("  ");
                    message.append(file);
                    message.append('\n');
                }
            }
            return message.toString();
        }
        return null;
    }
    
    //--

    public SVNURL getSvnurl() throws SVNException {
        return root.getRepository().getLocation().appendPath(path, true);
    }
    
    public static SvnNode fromWorkspace(FileNode workspace) throws LocatorException, IOException {
        return (SvnNode) workspace.getIO().node("svn:" + urlFromWorkspace(workspace));
    }

    public static String urlFromWorkspace(FileNode workspace) throws IOException {
        String url;

        workspace.join(".svn").checkExists();
        url = new Program(workspace, "svn", "info").exec();
        return extract(url, "URL:");
    }

    private static String extract(String str, String key) throws IOException {
        int start;
        int end;
 
        start = str.indexOf(key);
        if (start == - 1) {
            throw new IOException("missing " + key + " in " + str);
        }
        start += key.length();
        end = str.indexOf('\n', start);
        if (end == -1) {
            throw new IOException("missing newline in " + str);
        }
        return str.substring(start, end).trim();
    }
}
