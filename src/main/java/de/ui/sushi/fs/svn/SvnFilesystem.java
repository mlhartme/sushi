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

import de.ui.sushi.fs.Features;
import de.ui.sushi.fs.Filesystem;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.RootPathException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

public class SvnFilesystem extends Filesystem {
    static {
        FSRepositoryFactory.setup();
        DAVRepositoryFactory.setup();
        System.setProperty("svnkit.upgradeWC", "false"); // see https://wiki.svnkit.com/SVNKit_specific_system_properties
    }

    private String username;
    private String password;

    public SvnFilesystem(IO io, String name) {
        super(io, '/', new Features(false, false, false, false), name);

        this.username = null;
        this.password = null;
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }


    @Override
    public SvnRoot root(String url) throws RootPathException {
        try {
            return doRoot(url);
        } catch (SVNException e) {
            throw new RootPathException(e);
        }
    }

    public SvnRoot doRoot(String url) throws SVNException {
        String separator;
        SVNRepository repository;
        String root;

        separator = getSeparator();
        repository = repository(SVNURL.parseURIEncoded(url), username, password);
        root = repository.getRepositoryRoot(true).toString();
        if (!url.startsWith(root)) {
            throw new IllegalStateException(url + " vs " + root);
        }
        if (!root.endsWith(separator)) {
            root = root + separator;
            repository.setLocation(SVNURL.parseURIEncoded(root), true);
        }
        return root(repository);
    }

    @Override
    public String opaquePath(String url) throws RootPathException {
        try {
            return doOpaquePath(url);
        } catch (SVNException e) {
            throw new RootPathException(e);
        }
    }

    public String doOpaquePath(String url) throws SVNException {
        String separator;
        SVNRepository repository;
        String root;
        String result;

        separator = getSeparator();
        repository = repository(SVNURL.parseURIEncoded(url), username, password);
        root = repository.getRepositoryRoot(true).toString();
        if (!url.startsWith(root)) {
            throw new IllegalStateException(url + " vs " + root);
        }
        result = url.substring(root.length());
        if (result.length() > 0) {
            if (!result.startsWith(separator)) {
                throw new IllegalStateException(url + " vs " + root);
            }
            result = result.substring(separator.length());
        }
        return result;
    }

    public SvnRoot root(SVNRepository repository) throws SVNException {
        return new SvnRoot(this, repository);
    }

    //--

    public static SVNRepository repository(SVNURL url, String username, String password) throws SVNException {
        SVNRepository repository;

        repository = SVNRepositoryFactory.create(url);
        repository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager(
                SVNWCUtil.getDefaultConfigurationDirectory(),
                username, password,
                false /* do not store credentials, not even when configured */));
        return repository;
    }
}
