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
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import de.ui.sushi.fs.Root;
import de.ui.sushi.util.Base64;

public class WebdavRoot implements Root {
    private final WebdavFilesystem filesystem;
    public final HttpHost host;
    private final HttpParams params;
    private String authorization;

    public WebdavRoot(WebdavFilesystem filesystem, String protocol, String host, int port) {
        this.filesystem = filesystem;
        this.host = new HttpHost(host, port, protocol);
        this.authorization = null;
        this.params = new BasicHttpParams();

        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, WebdavFilesystem.ENCODING);
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

    public void setCredentials(String username, String password) {
    	authorization = "Basic " + new Base64(true).run(username + ":" + password);
    }

    public int getConnectionTimeout() {
        return HttpConnectionParams.getConnectionTimeout(params);
    }

    public void setConnectionTimeout(int millis) {
        HttpConnectionParams.setSoTimeout(params, millis);
    }

    public int getSoTimeout() {
        return HttpConnectionParams.getSoTimeout(params);
    }

    public void setSoTimeout(int millis) {
        HttpConnectionParams.setSoTimeout(params, millis);
    }

    @Override
    public boolean equals(Object obj) {
        WebdavRoot root;

        if (obj instanceof WebdavRoot) {
            root = (WebdavRoot) obj;
            return filesystem == root.filesystem /* TODO params etc */;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return host.hashCode();
    }

    public WebdavFilesystem getFilesystem() {
        return filesystem;
    }

    public String getId() {
        int port;

        port = host.getPort();
        // TODO: credentials?
        return "//" + host.getHostName() + (port == 80 ? "" : ":" + port) + "/";
    }

    public WebdavNode node(String path) {
        return new WebdavNode(this, path, false);
    }

    public String encodePath(String path) {
    	StringBuilder builder;

    	builder = new StringBuilder();
    	encodePath(path, builder);
    	return builder.toString();
    }

    private void encodePath(String path, StringBuilder dest) {
        for (String segment : filesystem.split(path)) {
            dest.append('/');
            try {
                dest.append(URLEncoder.encode(segment, WebdavFilesystem.ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    //--

    private final List<WebdavConnection> pool = new ArrayList<WebdavConnection>();

    public WebdavConnection allocate() throws IOException {
        int size;

        size = pool.size();
        if (size > 0) {
            return pool.remove(size - 1);
        } else {
        	Socket socket;

            if ("https".equals(host.getSchemeName())) {
                socket = SSLSocketFactory.getDefault().createSocket(host.getHostName(), host.getPort());
            } else {
                socket = new Socket(host.getHostName(), host.getPort());
            }
            return WebdavConnection.open(socket, params);
        }
    }

    public void free(HttpResponse response, WebdavConnection connection) throws IOException {
        HttpEntity entity;

        if (response != null) {
            entity = response.getEntity();
            if (entity != null) {
                entity.consumeContent();
            }
        }
        if (wantsClose(response)) {
            connection.close();
        }
        if (connection.isOpen() && pool.size() < 10) {
            pool.add(connection);
        }
    }

    private static boolean wantsClose(HttpResponse response) {
        if (response == null) {
            // no response yet
            return true;
        }
        for (Header header : response.getHeaders(HTTP.CONN_DIRECTIVE)) {
            if ("close".equalsIgnoreCase(header.getValue())) {
                return true;
            }
        }
        return false;
    }

    //--

    public void send(WebdavConnection conn, HttpRequest request) throws IOException {
        // TODO: side effect
        request.addHeader(HTTP.TARGET_HOST, host.getHostName());
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        // TODO: request.addHeader("Keep-Alive", "300");

        try {
            conn.sendRequestHeader(request);
            if (request instanceof HttpEntityEnclosingRequest) {
                conn.sendRequestEntity((HttpEntityEnclosingRequest) request);
            }
            conn.flush();
        } catch (IOException e) {
            free(null, conn);
            throw e;
        } catch (HttpException e) {
            free(null, conn);
            // TODO
          	throw new IOException(e);
        } catch (RuntimeException e) {
            free(null, conn);
            throw e;
        }
    }

    public HttpResponse receive(WebdavConnection conn) throws IOException {
        HttpResponse response;
        int statuscode;

        response = null;
        try {
            do {
                response = conn.receiveResponseHeader();
                if (canResponseHaveBody(response)) {
                    conn.receiveResponseEntity(response);
                }
                statuscode = response.getStatusLine().getStatusCode();
            } while (response == null || statuscode < HttpStatus.SC_OK);
            return response;
        } catch (IOException e) {
            free(response, conn);
            throw e;
        } catch (HttpException e) {
            free(response, conn);
            // TODO
          	throw new IOException(e);
        } catch (RuntimeException e) {
            free(response, conn);
            throw e;
        }
    }

    private boolean canResponseHaveBody(HttpResponse response) {
        int status;

        status = response.getStatusLine().getStatusCode();
        return status >= HttpStatus.SC_OK
            && status != HttpStatus.SC_NO_CONTENT
            && status != HttpStatus.SC_NOT_MODIFIED
            && status != HttpStatus.SC_RESET_CONTENT;
    }
}
