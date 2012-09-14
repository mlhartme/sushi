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

import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.net.URI;

/** Nodes accessible via svn. Uses svnkit for server-side communication, but not to interact with local checkouts. */
public class SvnFilesystem extends Filesystem {
    static {
        FSRepositoryFactory.setup();
        DAVRepositoryFactory.setup();
        System.setProperty("svnkit.upgradeWC", "false"); // see https://wiki.svnkit.com/SVNKit_specific_system_properties
    }

    private ISVNAuthenticationManager defaultAuthenticationManager;

    public SvnFilesystem(World world, String name) {
        super(world, new Features(true, false, false, false, false, false, true), name);

        this.defaultAuthenticationManager = authenticationManager(null, null);
    }

    public ISVNAuthenticationManager getDefaultAuthenticationManager() {
        return defaultAuthenticationManager;
    }

    public void setDefaultCredentials(String username, String password) {
        setDefaultAuthenticationManager(authenticationManager(username, password));
    }

    public void setDefaultAuthenticationManager(ISVNAuthenticationManager authenticationManager) {
        this.defaultAuthenticationManager = authenticationManager;
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
        separator = SEPARATOR_STRING;
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
                throw new NodeInstantiationException(uri, "invalid tailing " + separator);
            }
            if (encodedPath.startsWith(separator)) {
                throw new NodeInstantiationException(uri, "invalid heading " + separator);
            }
            repository = repository(encodedSchemeSpecific.substring(0, encodedSchemeSpecific.length() - encodedPath.length()));
            return new SvnRoot(this, repository).node(SVNEncodingUtil.uriDecode(encodedPath), null);
        } catch (SVNException e) {
            throw new NodeInstantiationException(uri, e.getMessage(), e);
        }
    }

    //--

    public SVNRepository repository(String urlstr) throws SVNException {
        ISVNAuthenticationManager authenticationManager;
        String userinfo;
        SVNURL url;
        int idx;
        String username;
        String password;

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
            authenticationManager = authenticationManager(username, password);
        } else {
            authenticationManager = defaultAuthenticationManager == null ? authenticationManager(null, null) : defaultAuthenticationManager;
        }

        return repository(url, authenticationManager);
    }

    //--

    public static SVNRepository repository(SVNURL url, ISVNAuthenticationManager authenticationManager) throws SVNException {
        SVNRepository repository;

        repository = SVNRepositoryFactory.create(url);
        repository.setAuthenticationManager(authenticationManager);
        return repository;
    }

    public static ISVNAuthenticationManager authenticationManager(String username, String password) {
        return SVNWCUtil.createDefaultAuthenticationManager(
                SVNWCUtil.getDefaultConfigurationDirectory(),
                username, password,
                false /* do not store credentials, not even when configured */);
    }
}
