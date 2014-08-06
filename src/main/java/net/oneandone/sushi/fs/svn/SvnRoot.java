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

import net.oneandone.sushi.fs.Root;
import net.oneandone.sushi.fs.file.FileNode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnRoot implements Root<SvnNode> {
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
        return clientManager.getWCClient().doInfo(node.toPath().toFile(), SVNRevision.WORKING);
    }

    public SvnNode node(String path, String encodedQuery) {
        if (encodedQuery != null) {
            throw new IllegalArgumentException(encodedQuery);
        }
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
     
    public void dispose() {
        clientManager.dispose();
        repository.closeSession();
    }
}
