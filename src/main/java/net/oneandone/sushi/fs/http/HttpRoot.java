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
import net.oneandone.sushi.fs.http.methods.Method;
import net.oneandone.sushi.fs.http.model.Body;
import net.oneandone.sushi.fs.http.model.Header;
import net.oneandone.sushi.fs.http.model.Request;
import net.oneandone.sushi.fs.http.model.Response;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

    public boolean getTcpNoDelay() {
        return true;
    }

    public int getLinger() {
        return -1;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(final int timeout) {
        soTimeout = timeout;

    }

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
        	Socket socket;

            if ("https".equals(protocol)) {
                socket = SSLSocketFactory.getDefault().createSocket(hostname, port);
            } else {
                socket = new Socket(hostname, port);
            }
            return HttpConnection.open(socket, this);
        }
    }

    public synchronized void free(Response response, HttpConnection connection) throws IOException {
        Body body;

        if (allocated == 0) {
            throw new IllegalStateException();
        }
        allocated--;
        if (response != null) {
            body = response.getBody();
            if (body != null) {
                body.content.close();
            }
        }
        if (wantsClose(response)) {
            connection.close();
        }
        if (connection.isOpen() && pool.size() < 10) {
            pool.add(connection);
        }
    }

    public synchronized int getAllocated() {
        return allocated;
    }

    private static boolean wantsClose(Response response) {
        Header header;

        if (response == null) {
            // no response yet
            return true;
        }
        header = response.getHeaderList().getFirst(Header.CONNECTION);
        return header != null && "close".equalsIgnoreCase(header.value);
    }

    //--

    public void send(HttpConnection conn, Request request) throws IOException {
        // TODO: side effect
        request.headerList.add(Header.HOST, hostname);
        if (authorization != null) {
            request.headerList.add("Authorization", authorization);
        }
        // TODO: request.addHeader("Keep-Alive", "300");

        try {
            conn.sendRequestHeader(request);
            conn.sendRequestBody(request);
            conn.flush();
        } catch (IOException | RuntimeException e) {
            free(null, conn);
            throw e;
        }
    }

    public Response receive(HttpConnection connection, boolean head) throws IOException {
        Response response;
        int statuscode;

        response = null;
        try {
            do {
                response = connection.receiveResponseHeader();
                if (canResponseHaveBody(response, head)) {
                    connection.receiveResponseBody(response);
                }
                statuscode = response.getStatusLine().statusCode;
            } while (statuscode < Method.STATUSCODE_OK);
            return response;
        } catch (IOException | RuntimeException e) {
            free(response, connection);
            throw e;
        }
    }

    private boolean canResponseHaveBody(Response response, boolean head) {
        int status;

        status = response.getStatusLine().statusCode;
        if (status == Method.STATUSCODE_OK && head) {
            return false;
        }
        return status >= Method.STATUSCODE_OK
            && status != Method.STATUSCODE_NO_CONTENT
            && status != Method.STATUSCODE_NOT_MODIFIED
            && status != Method.STATUSCODE_RESET_CONTENT;
    }
}
