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

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
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
    private final Map<String, Proxy> proxies;

    public HttpFilesystem(World io, String scheme) {
        super(io, new Features(true, true, false, false, false, false, false), scheme);

        this.defaultConnectionTimeout = 0;
        this.defaultSoTimeout = 0;
        this.defaultDav = null;
        this.socketFactorySelector = HttpFilesystem::defaultSocketFactorySelector;
        this.proxies = new HashMap<>();
        addProxy("http", proxies);
        addProxy("https", proxies);
    }

    private static void addProxy(String scheme, Map<String, Proxy> result) {
        Proxy pp;

        pp = Proxy.forPropertiesOpt(scheme);
        if (pp != null) {
            result.put(scheme, pp);
        }
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

    public void setProxy(String scheme, Proxy pp) {
        proxies.put(scheme, pp);
    }

    public Proxy getProxy(String scheme) {
        return proxies.get(scheme);
    }

    /**
     * return proxy url if configured for this filesystem:
     */
    public URI proxy(URI uri) {
        Proxy conf;
        String scheme;

        scheme = uri.getScheme();
        conf = proxies.get(scheme);
        if (conf == null || conf.excludes(uri)) {
            return null;
        }
        try {
            return new URI(scheme, null, conf.host, conf.port, null, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
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
