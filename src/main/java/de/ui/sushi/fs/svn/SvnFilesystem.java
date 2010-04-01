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

import java.net.URI;

public class SvnFilesystem extends Filesystem {
    static {
        FSRepositoryFactory.setup();
        DAVRepositoryFactory.setup();
        System.setProperty("svnkit.upgradeWC", "false"); // see https://wiki.svnkit.com/SVNKit_specific_system_properties
    }

    private String username;
    private String password;

    public SvnFilesystem(IO io, String name) {
        super(io, '/', new Features(true, false, false, false, false, false), name);

        this.username = null;
        this.password = null;
    }

    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public SvnNode node(URI uri) throws RootPathException {
        String schemeSpecific;
        String path;
        String separator;
        String root;
        SVNRepository repository;

        checkOpaque(uri);
        separator = getSeparator();
        schemeSpecific = uri.getSchemeSpecificPart();
        try {
            root = repository(SVNURL.parseURIEncoded(schemeSpecific), username, password).getRepositoryRoot(true).toString();
            if (!schemeSpecific.startsWith(root)) {
                throw new IllegalStateException(schemeSpecific + " vs " + root);
            }
            path = schemeSpecific.substring(root.length());
            if (path.length() > 0) {
                if (!path.startsWith(separator)) {
                    throw new IllegalStateException(schemeSpecific + " vs " + root);
                }
                path = path.substring(separator.length());
            }
            if (path.endsWith(separator)) {
                throw new RootPathException(uri, "invalid tailing " + getSeparator());
            }
            if (path.startsWith(separator)) {
                throw new RootPathException(uri, "invalid heading " + getSeparator());
            }
            repository = repository(SVNURL.parseURIEncoded(schemeSpecific.substring(0, schemeSpecific.length() - path.length())), username, password);
            return root(repository).node(path);
        } catch (SVNException e) {
            throw new RootPathException(uri, e.getMessage(), e);
        }
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
