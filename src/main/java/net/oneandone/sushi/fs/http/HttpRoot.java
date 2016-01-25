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

import net.oneandone.sushi.fs.Root;
import net.oneandone.sushi.fs.http.io.AsciiInputStream;
import net.oneandone.sushi.fs.http.io.AsciiOutputStream;
import net.oneandone.sushi.fs.http.methods.Method;
import net.oneandone.sushi.fs.http.model.Header;
import net.oneandone.sushi.fs.http.model.HeaderList;
import net.oneandone.sushi.fs.http.model.ProtocolException;
import net.oneandone.sushi.fs.http.model.Request;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.io.LineLogger;
import net.oneandone.sushi.io.LoggingAsciiInputStream;
import net.oneandone.sushi.io.LoggingAsciiOutputStream;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;

public class HttpRoot implements Root<HttpNode> {
    private final HttpFilesystem filesystem;
    private final String hostname;
    private final int port;
    private final String protocol;

    // configuration
    private int soTimeout = 0;
    private int connectionTimeout = 0;
    private String authorization;

    public HttpRoot(HttpFilesystem filesystem, String protocol, String hostname, int port) {
        this.filesystem = filesystem;
        this.protocol = protocol;
        this.hostname = hostname;
        this.port = port;
        this.authorization = null;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public void setUserInfo(String userinfo) {
        int idx;

        idx = userinfo.indexOf(':');
        if (idx == -1) {
            setCredentials(userinfo, "");
        } else {
            setCredentials(userinfo.substring(0, idx), userinfo.substring(idx + 1));
        }
    }

    //-- configuration

    public void setCredentials(String username, String password) {
    	authorization = "Basic " + Base64.getEncoder().encodeToString(filesystem.getWorld().getSettings().bytes(username + ":" + password));
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int timeout) {
        connectionTimeout = timeout;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(final int timeout) {
        soTimeout = timeout;

    }

    public HttpFilesystem getFilesystem() {
        return filesystem;
    }

    public String getId() {
        // TODO: credentials?
        return "//" + hostname + (port == 80 ? "" : ":" + port) + "/";
    }

    public HttpNode node(String path, String encodedQuery) {
        return new HttpNode(this, path, encodedQuery, false);
    }

    private final List<HttpConnection> pool = new ArrayList<>();
    private int allocated = 0;

    public synchronized HttpConnection allocate() throws IOException {
        int size;

        allocated++;
        size = pool.size();
        if (size > 0) {
            return pool.remove(size - 1);
        } else {
            return connect();
        }
    }

    public synchronized void free(HttpConnection connection) throws IOException {
        if (allocated == 0) {
            throw new IllegalStateException();
        }
        allocated--;
        if (connection.isOpen() && pool.size() < 10) {
            pool.add(connection);
        }
    }

    public synchronized int getAllocated() {
        return allocated;
    }

    public HttpConnection connect() throws IOException {
        Socket socket;
        int buffersize;
        InputStream input;
        OutputStream output;
        URL proxyUrl;

        if ("https".equals(protocol)) {
            socket = SSLSocketFactory.getDefault().createSocket(hostname, port);
        } else {
            proxyUrl = proxyUrl();
            socket = proxyUrl != null ? proxyconnect(proxyUrl) : new Socket(hostname, port);
        }
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(soTimeout);
        buffersize = Math.max(socket.getReceiveBufferSize(), 1024);
        input = socket.getInputStream();
        output = socket.getOutputStream();
        if (HttpFilesystem.WIRE.isLoggable(Level.FINE)) {
            input = new LoggingAsciiInputStream(input, new LineLogger(HttpFilesystem.WIRE, "<<< "));
            output = new LoggingAsciiOutputStream(output, new LineLogger(HttpFilesystem.WIRE, ">>> "));
        }
        return new HttpConnection(socket, new AsciiInputStream(input, buffersize), new AsciiOutputStream(output, buffersize));
    }

    private URL proxyUrl() {
        String url;

        url = System.getenv("HTTP_PROXY");
        if (url == null) {
            return null;
        }
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    private Socket proxyconnect(URL proxyUrl) throws IOException {
        Request request;
        Socket proxy;
        InputStream input;
        OutputStream output;
        AsciiOutputStream aOut;
        AsciiInputStream aIn;
        Response response;

        request = new Request("CONNECT", hostname, null);
        proxy = new Socket(proxyUrl.getHost(), proxyUrl.getPort());
        input = proxy.getInputStream();
        output = proxy.getOutputStream();
        if (HttpFilesystem.WIRE.isLoggable(Level.FINE)) {
            input = new LoggingAsciiInputStream(input, new LineLogger(HttpFilesystem.WIRE, "<<< "));
            output = new LoggingAsciiOutputStream(output, new LineLogger(HttpFilesystem.WIRE, ">>> "));
        }
        aOut = new AsciiOutputStream(output, 1024);
        aIn = new AsciiInputStream(input, 1024);
        request.write(aOut);
        aOut.flush();
        response = Response.parse(aIn);
        if (response.getStatusLine().statusCode != Method.STATUSCODE_OK) {
            throw new ProtocolException("connect failed: " + response.getStatusLine());
        }
        return proxy;
    }

    public void addDefaultHeader(HeaderList headerList) {
        headerList.add(Header.HOST, hostname);
        if (authorization != null) {
            headerList.add("Authorization", authorization);
        }
        headerList.add("Expires", "0");
        headerList.add("Pragma", "no-cache");
        headerList.add("Cache-control", "no-cache");
        headerList.add("Cache-store", "no-store");
        headerList.add("User-Agent", "Sushi Http");
    }

    //--

    @Override
    public boolean equals(Object obj) {
        HttpRoot root;

        if (obj instanceof HttpRoot) {
            root = (HttpRoot) obj;
            return filesystem == root.filesystem /* TODO params etc */;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hostname.hashCode();
    }
}