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

package com.oneandone.sushi.fs.webdav;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.HttpConnectionMetricsImpl;
import org.apache.http.impl.entity.EntityDeserializer;
import org.apache.http.impl.entity.EntitySerializer;
import org.apache.http.impl.entity.LaxContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.HttpRequestWriter;
import org.apache.http.impl.io.HttpResponseParser;
import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class WebdavConnection implements HttpClientConnection {
    public static WebdavConnection open(Socket socket, HttpParams params) throws IOException {
        int linger;
        int buffersize;
        SocketInputBuffer input;
        SocketOutputBuffer output;
        
        socket.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(params));
        socket.setSoTimeout(HttpConnectionParams.getSoTimeout(params));
        linger = HttpConnectionParams.getLinger(params);
        if (linger >= 0) {
            socket.setSoLinger(linger > 0, linger);
        }
        buffersize = HttpConnectionParams.getSocketBufferSize(params);
		if (WebdavFilesystem.WIRE.isLoggable(Level.FINE)) {
			input = new LoggingSessionInputBuffer(socket, buffersize, params, WebdavFilesystem.WIRE);
            output = new LoggingSessionOutputBuffer(socket, buffersize, params, WebdavFilesystem.WIRE);
		} else {
	        input = new SocketInputBuffer(socket, buffersize, params);
            output = new SocketOutputBuffer(socket, buffersize, params);
		}
        return new WebdavConnection(socket, input, output, params);
    }

    private final Socket socket;
    private final EntitySerializer entityserializer;
    private final EntityDeserializer entitydeserializer;
    private final SocketInputBuffer input;
    private final SocketOutputBuffer output;
    private final HttpMessageParser responseParser;
    private final HttpMessageWriter requestWriter;
    private final HttpConnectionMetricsImpl metrics;
	private boolean open;

    public WebdavConnection(Socket socket, SocketInputBuffer input, SocketOutputBuffer output, HttpParams params) {
    	this.socket = socket;
        this.entityserializer = new EntitySerializer(new StrictContentLengthStrategy());
        this.entitydeserializer = new EntityDeserializer(new LaxContentLengthStrategy());
        this.input = input;
        this.output = output;
        this.responseParser = new HttpResponseParser(input, null, new DefaultHttpResponseFactory(), params);
        this.requestWriter = new HttpRequestWriter(output, null, params);
        this.metrics = new HttpConnectionMetricsImpl(input.getMetrics(), output.getMetrics());
        this.open = true;
    }

    //-- HttpClientConnection implementation
    
    public boolean isResponseAvailable(int timeout) throws IOException {
        return input.isDataAvailable(timeout);
    }

    public void sendRequestHeader(final HttpRequest request) throws HttpException, IOException {
        requestWriter.write(request);
        metrics.incrementRequestCount();
    }

    public void sendRequestEntity(final HttpEntityEnclosingRequest request) throws HttpException, IOException {
        if (request.getEntity() != null) {
        	entityserializer.serialize(output, request, request.getEntity());
        }
    }

    public HttpResponse receiveResponseHeader() throws HttpException, IOException {
        HttpResponse response = (HttpResponse) responseParser.parse();
        if (response.getStatusLine().getStatusCode() >= 200) {
            metrics.incrementResponseCount();
        }
        return response;
    }
    
    public void receiveResponseEntity(final HttpResponse response) throws HttpException, IOException {
        response.setEntity(entitydeserializer.deserialize(input, response));
    }
    
    public void flush() throws IOException {
        output.flush();
    }

    //-- HttpConnection implementation
    
    public void close() throws IOException {
        if (!open) {
            return;
        }
        open = false;
        socket.close();
    }
    
    public boolean isOpen() {
        return open;
    }
    
    public boolean isStale() {
        if (!isOpen() || isEof()) {
            return true;
        }
        try {
            input.isDataAvailable(1);
            return false;
        } catch (IOException ex) {
            return true;
        }
    }
    
    public void setSocketTimeout(int timeout) {
        try {
            socket.setSoTimeout(timeout);
        } catch (SocketException e) {
        	throw new RuntimeException("TODO", e);
        }
    }
    
    public int getSocketTimeout() {
        try {
            return socket.getSoTimeout();
        } catch (SocketException e) {
        	throw new RuntimeException("TODO", e);
        }
    }

    /** CAUTION: HttpCore and Java Sockets have a different terminology here */
    public void shutdown() throws IOException {
        open = false;
        socket.close();
    }
    
    public HttpConnectionMetrics getMetrics() {
        return metrics;
    }

    //--
    
    private boolean isEof() {
        return input.isEof();
    }
    
    public SocketOutputBuffer getOutputBuffer() {
    	return output;
    }
    
	@Override
    public String toString() {
		return "WebdavConnection(" + socket.getPort() + ')';
    }
}