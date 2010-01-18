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

package de.ui.sushi.fs.webdav;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.ui.sushi.fs.Features;
import de.ui.sushi.fs.Filesystem;
import de.ui.sushi.fs.IO;
import de.ui.sushi.fs.RootPathException;

public class WebdavFilesystem extends Filesystem {
	public static final String ENCODING = "UTF-8";
	public static final Logger WIRE = Logger.getLogger("sushi.webdav.wire");

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

    private int defaultConnectionTimeout;
    private int defaultSoTimeout;

    public WebdavFilesystem(IO io, String name) {
        super(io, '/', new Features(false, true, false, true, false, false), name);

        this.defaultConnectionTimeout = 0;
        this.defaultSoTimeout = 0;
    }

    @Override
    public WebdavRoot root(String root) throws RootPathException {  // TODO
        URL url;

        try {
            url = new URL(getScheme() + "://" + root + "/");
        } catch (MalformedURLException e) {
            throw new RootPathException(e);
        }
        return root(url);
    }

    public WebdavRoot root(URL url) {
        WebdavRoot result;
        String info;
        int port;

        if (url.getRef() != null) {
            throw new IllegalArgumentException(url.toString());
        }
        if (url.getQuery() != null) {
            throw new IllegalArgumentException(url.toString());
        }
        // ignores url.getPath()
        port = url.getPort();
        if (port == -1) {
        	port = "https".equals(url.getProtocol()) ? 443 : 80;
        }
        result = new WebdavRoot(this, url.getProtocol(), url.getHost(), port);
        info = url.getUserInfo();
        if (info != null) {
        	// TODO
        	info = info.replace('$', '@');
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
