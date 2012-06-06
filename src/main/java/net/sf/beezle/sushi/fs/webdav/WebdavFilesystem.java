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

package net.sf.beezle.sushi.fs.webdav;

import net.sf.beezle.sushi.fs.Features;
import net.sf.beezle.sushi.fs.Filesystem;
import net.sf.beezle.sushi.fs.NodeInstantiationException;
import net.sf.beezle.sushi.fs.Settings;
import net.sf.beezle.sushi.fs.World;
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
