package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.util.Separator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Proxy {
    private static final Separator NON_PROXY_SEP = Separator.on('|').trim().skipEmpty();

    /**
     * load properties configured by standard system properties
     * https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html
     */
    public static Proxy forPropertiesOpt(String scheme) {
        String host;
        String port;
        Proxy result;

        host = System.getProperty(scheme + ".proxyHost");
        if (host == null) {
            return null;
        }
        port = System.getProperty(scheme + ".proxyPort");
        if (port == null) {
            throw new IllegalStateException("missing proxy port for host " + host);
        }
        result = new Proxy(host, Integer.parseInt(port));
        result.excludes.addAll(NON_PROXY_SEP.split(System.getProperty(scheme + ".nonProxyHosts", "")));
        return result;
    }

    public static Proxy forEnvOpt(String scheme) {
        return forEnvOpt(System.getenv(scheme + "_proxy"), System.getenv("no_proxy"));
    }

    /**
     * Turn Java proxy properties for http_proxy and no_proxy environment variables into the
     * respective Java properties. See
     * - http://wiki.intranet.1and1.com/bin/view/UE/HttpProxy
     * - http://info4tech.wordpress.com/2007/05/04/java-http-proxy-settings/
     * - http://download.oracle.com/javase/6/docs/technotes/guides/net/proxies.html
     * - http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html#Proxies
     */
    private static Proxy forEnvOpt(String proxy, String noProxy) {
        URI uri;
        int port;
        Proxy result;

        if (proxy == null) {
            return null;
        }
        try {
            uri = new URI(proxy);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("invalid value for http_proxy: " + proxy, e);
        }
        port = uri.getPort();
        if (port == -1) {
            port = 80;
        }
        result = new Proxy(uri.getHost(), port);
        for (String exclude : Separator.COMMA.split(noProxy == null ? "" : noProxy)) {
            if (exclude.startsWith(".")) {
                exclude = '*' + exclude;
            }
            result.excludes.add(exclude);
        }
        return result;
    }

    //--

    public final String host;
    public final int port;
    public final List<String> excludes;

    public Proxy(String host, int port) {
        this.host = host;
        this.port = port;
        this.excludes = new ArrayList<>();
    }

    public boolean excludes(URI uri) {
        String host;

        host = uri.getHost();
        for (String exclude : excludes) {
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
        return false;
    }

}
