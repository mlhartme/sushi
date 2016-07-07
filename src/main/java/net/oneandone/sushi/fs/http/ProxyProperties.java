package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.util.Separator;
import net.oneandone.sushi.util.Strings;

import java.net.URI;
import java.util.Map;

public class ProxyProperties {
    public static void addSystemOpt(String scheme, Map<String, ProxyProperties> result) {
        ProxyProperties pp;

        pp = forSystemOpt(scheme);
        if (pp != null) {
            result.put(scheme, pp);
        }
    }

    private static final Separator NON_PROXY_SEP = Separator.on('|').trim().skipEmpty();

    /**
     * load properties configured by standard system properties
     * https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html
     */
    public static ProxyProperties forSystemOpt(String scheme) {
        String host;
        String port;
        String[] excludes;

        host = System.getProperty(scheme + ".proxyHost");
        if (host == null) {
            return null;
        }
        port = System.getProperty(scheme + ".proxyPort");
        if (port == null) {
            throw new IllegalStateException("missing proxy port for host " + host);
        }
        excludes = Strings.toArray(NON_PROXY_SEP.split(System.getProperty(scheme + ".nonProxyHosts", "")));
        return new ProxyProperties(host, Integer.parseInt(port), excludes);
    }

    public final String host;
    public final int port;
    public final String[] excludes;

    public ProxyProperties(String host, int port, String[] excludes) {
        this.host = host;
        this.port = port;
        this.excludes = excludes;
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
