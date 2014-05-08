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
package net.oneandone.sushi.fs.webdav;

import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.Settings;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.NetRc;
import org.apache.http.HttpHost;

import java.io.IOException;
import java.net.URI;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class WebdavFilesystem extends Filesystem {
	public static final String ENCODING = Settings.UTF_8;
	public static final Logger WIRE = Logger.getLogger("sushi.webdav.wire");

    static {
        // make sure that WebdavFilesystem class loading fails if http core dependency is not available
        if (HttpHost.class == null) {
            throw new IllegalStateException();
        }
    }

	public static void wireLog(String file) {
        Handler handler;

        WIRE.setLevel(Level.FINE);
        try {
            handler = new FileHandler(file, false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        handler.setFormatter(new Formatter() {
                    @Override
                    public String format(LogRecord record) {
                        String message;
                        Throwable e;
                        StringBuilder result;

                        message = record.getMessage();
                        result = new StringBuilder(message.length() + 1);
                        result.append(message);
                        result.append('\n');
                        e = record.getThrown();
                        if (e != null) {
                            // TODO getStacktrace(e, result);
                        }
                        return result.toString();
                    }
                });

        WIRE.addHandler(handler);
	}

    private final String internalScheme;
    private final boolean dav;
    private int defaultConnectionTimeout;
    private int defaultSoTimeout;

    public WebdavFilesystem(World io, String scheme, String internalScheme, boolean dav) {
        super(io, new Features(dav, true, false, false, false, false, false), scheme);

        this.internalScheme = internalScheme;
        this.dav = dav;
        this.defaultConnectionTimeout = 0;
        this.defaultSoTimeout = 0;
    }

    @Override
    public WebdavNode node(URI uri, Object extra) throws NodeInstantiationException {
        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        if (uri.getFragment() != null) {
            throw new NodeInstantiationException(uri, "unexpected path fragment");
        }
        if (uri.isOpaque()) {
            throw new NodeInstantiationException(uri, "uri is not hierarchical");
        }
        return root(uri).node(getCheckedPath(uri), uri.getRawQuery());
    }

    public String getInternalScheme() {
        return internalScheme;
    }

    public boolean isDav() {
        return dav;
    }

    public WebdavRoot root(URI uri) {
        WebdavRoot result;
        String info;
        int port;
        NetRc.NetRcAuthenticator authenticator;

        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(uri.toString());
        }
        // ignores url.getPath()
        port = uri.getPort();
        if (port == -1) {
        	port = "https".equals(uri.getScheme()) ? 443 : 80;
        }
        result = new WebdavRoot(this, internalScheme, uri.getHost(), port);
        info = uri.getUserInfo();
        if (info != null) {
            result.setUserInfo(info);
        } else {
            authenticator = getWorld().getNetRc().getAuthenticators(uri.getHost());
            if (authenticator != null) {
                result.setCredentials(authenticator.getUser(), authenticator.getPass());
            }
        }
        return result;
    }

    public int getDefaultConnectionTimeout() {
        return defaultConnectionTimeout;
    }

    public void setDefaultConnectionTimeout(int millis) {
        defaultConnectionTimeout = millis;
    }

    public int getDefaultSoTimeout() {
        return defaultSoTimeout;
    }

    public void setDefaultReadTimeout(int millis) {
        defaultSoTimeout = millis;
    }
}
