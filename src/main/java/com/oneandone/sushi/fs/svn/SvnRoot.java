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

import de.ui.sushi.fs.Root;
import de.ui.sushi.fs.file.FileNode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnRoot implements Root {
    private final SvnFilesystem filesystem;
    private final SVNRepository repository;
    private final SVNClientManager clientManager;
    private String comment;

    public SvnRoot(SvnFilesystem filesystem, SVNRepository repository) {
        this.filesystem = filesystem;
        this.repository = repository;
        this.clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), repository.getAuthenticationManager());
        this.comment = "sushi commit";
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public SvnFilesystem getFilesystem() {
        return filesystem;
    }

    public String getId() {
        return repository.getLocation().toString() + "/";
    }

    public SVNClientManager getClientMananger() {
        return clientManager;
    }

    public SVNRepository getRepository() {
        return repository;
    }

    public SVNInfo getInfo(FileNode node) throws SVNException {
        return clientManager.getWCClient().doInfo(node.getFile(), SVNRevision.WORKING);
    }

    public SvnNode node(String path) {
        return new SvnNode(this, path);
    }

    @Override
    public boolean equals(Object obj) {
        SvnRoot root;

        if (obj instanceof SvnRoot) {
            root = (SvnRoot) obj;
            return repository.getLocation().equals(root.repository.getLocation());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return repository.getLocation().hashCode();
    }
}
