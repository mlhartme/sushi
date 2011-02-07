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

package com.oneandone.sushi.fs.svn;

import com.oneandone.sushi.fs.Features;
import com.oneandone.sushi.fs.Filesystem;
import com.oneandone.sushi.fs.NodeInstantiationException;
import com.oneandone.sushi.fs.World;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
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

    private String defaultUsername;
    private String defaultPassword;

    public SvnFilesystem(World world, String name) {
        super(world, '/', new Features(true, false, false, false, false, false), name);

        this.defaultUsername = null;
        this.defaultPassword = null;
    }

    public void setDefaultCredentials(String username, String password) {
        this.defaultUsername = username;
        this.defaultPassword = password;
    }

    @Override
    public SvnNode node(URI uri, Object extra) throws NodeInstantiationException {
        String encodedSchemeSpecific;
        String encodedPath;
        String separator;
        String root;
        SVNRepository repository;

        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkOpaque(uri);
        separator = getSeparator();
        encodedSchemeSpecific = uri.getRawSchemeSpecificPart();
        try {
            repository = repository(encodedSchemeSpecific);
            root = repository.getRepositoryRoot(true).toString();
            if (!encodedSchemeSpecific.startsWith(root)) {
                throw new IllegalStateException(encodedSchemeSpecific + " vs " + root);
            }
            encodedPath = encodedSchemeSpecific.substring(root.length());
            if (encodedPath.length() > 0) {
                if (!encodedPath.startsWith(separator)) {
                    throw new IllegalStateException(encodedSchemeSpecific + " vs " + root);
                }
                encodedPath = encodedPath.substring(separator.length());
            }
            if (encodedPath.endsWith(separator)) {
                throw new NodeInstantiationException(uri, "invalid tailing " + getSeparator());
            }
            if (encodedPath.startsWith(separator)) {
                throw new NodeInstantiationException(uri, "invalid heading " + getSeparator());
            }
            repository = repository(encodedSchemeSpecific.substring(0, encodedSchemeSpecific.length() - encodedPath.length()));
            return new SvnRoot(this, repository).node(SVNEncodingUtil.uriDecode(encodedPath), null);
        } catch (SVNException e) {
            throw new NodeInstantiationException(uri, e.getMessage(), e);
        }
    }

    //--

    public SVNRepository repository(String url) throws SVNException {
        return repository(url, defaultUsername, defaultPassword);
    }

    public static SVNRepository repository(String urlstr, String username, String password) throws SVNException {
        SVNRepository repository;
        String userinfo;
        SVNURL url;
        int idx;

        url = SVNURL.parseURIEncoded(urlstr);
        userinfo = url.getUserInfo();
        if (userinfo != null) {
            idx = userinfo.indexOf(':');
            if (idx == -1) {
                username = userinfo;
                password = null;
            } else {
                username = userinfo.substring(0, idx);
                password = userinfo.substring(idx + 1);
            }
        }
        repository = SVNRepositoryFactory.create(url);
        repository.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager(
                SVNWCUtil.getDefaultConfigurationDirectory(),
                username, password,
                false /* do not store credentials, not even when configured */));
        return repository;
    }
}
