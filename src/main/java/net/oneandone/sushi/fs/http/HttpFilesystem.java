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
package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.util.NetRc;
import net.oneandone.sushi.util.Separator;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class HttpFilesystem extends Filesystem {
	public static final Logger WIRE = Logger.getLogger("sushi.http.wire");

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
    private Boolean defaultDav;
    private BiFunction<String, String, SocketFactory> socketFactorySelector;

    public HttpFilesystem(World io, String scheme) {
        super(io, new Features(true, true, false, false, false, false, false), scheme);

        this.defaultConnectionTimeout = 0;
        this.defaultSoTimeout = 0;
        this.defaultDav = null;
        this.socketFactorySelector = HttpFilesystem::defaultSocketFactorySelector;
    }

    public BiFunction<String, String, SocketFactory> getSocketFactorySelector() {
        return socketFactorySelector;
    }

    public void setSocketFactorySelector(BiFunction<String, String, SocketFactory> socketFactorySelector) {
        this.socketFactorySelector = socketFactorySelector;
    }

    public static SocketFactory defaultSocketFactorySelector(String protocol, String hostname) {
        return "https".equals(protocol) ? SSLSocketFactory.getDefault() : null;
    }

    @Override
    public HttpNode node(URI uri, Object extra) throws NodeInstantiationException {
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

    public HttpRoot root(URI uri) {
        HttpRoot result;
        String info;
        int port;
        NetRc.Authenticator authenticator;

        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(uri.toString());
        }
        // ignores url.getPath()
        port = uri.getPort();
        if (port == -1) {
        	port = "https".equals(uri.getScheme()) ? 443 : 80;
        }
        result = new HttpRoot(this, getScheme(), uri.getHost(), port, proxy(uri), defaultDav);
        info = uri.getUserInfo();
        if (info != null) {
            result.setUserInfo(info);
        } else {
            authenticator = getWorld().getNetRc().getAuthenticator(uri.getHost());
            if (authenticator != null) {
                result.setCredentials(authenticator.getUser(), authenticator.getPass());
            }
        }
        return result;
    }

    /**
     * return proxy url if configured by java standard properties:
     * https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html
     */
    public static URI proxy(URI uri) {
        String scheme;
        String proxyHost;
        String proxyPort;

        scheme = uri.getScheme();
        proxyHost = System.getProperty(scheme + ".proxyHost");
        if (proxyHost == null) {
            return null;
        }
        proxyPort = System.getProperty(scheme + ".proxyPort");
        if (proxyPort == null) {
            throw new IllegalStateException("missing proxy port for host " + proxyHost);
        }
        if (excludeProxy(uri, System.getProperty(scheme + ".nonProxyHosts"))) {
            return null;
        }
        try {
            return new URI(scheme, null, proxyHost, Integer.parseInt(proxyPort), null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final Separator NON_PROXY_SEP = Separator.on('|').trim().skipEmpty();

    public static boolean excludeProxy(URI uri, String nonProxyHosts) {
        String host;

        if (nonProxyHosts != null) {
            host = uri.getHost();
            for (String exclude : NON_PROXY_SEP.split(nonProxyHosts)) {
                if (exclude.startsWith("*")) {
                    if (host.endsWith(exclude.substring(1))) {
                        return true;
                    }
                } else {
                    if (host.equals(exclude)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Boolean getDefaultDav() {
        return defaultDav;
    }

    public void setDefaultDav(Boolean dav) {
        defaultDav = dav;
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
