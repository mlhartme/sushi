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
package net.oneandone.sushi.fs.webdav;

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
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.HttpRequestWriter;
import org.apache.http.impl.io.SocketInputBuffer;
import org.apache.http.impl.io.SocketOutputBuffer;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.io.SessionOutputBuffer;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;

public class WebdavConnection implements HttpClientConnection {
    public static WebdavConnection open(Socket socket, HttpParams params) throws IOException {
        int linger;
        int buffersize;
        SessionInputBuffer input;
        SessionOutputBuffer output;
        
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
    private final SessionInputBuffer input;
    private final SessionOutputBuffer output;
    private final HttpMessageParser responseParser;
    private final HttpMessageWriter requestWriter;
    private final HttpConnectionMetricsImpl metrics;
	private boolean open;

    public WebdavConnection(Socket socket, SessionInputBuffer input, SessionOutputBuffer output, HttpParams params) {
    	this.socket = socket;
        this.entityserializer = new EntitySerializer(new StrictContentLengthStrategy());
        this.entitydeserializer = new EntityDeserializer(new LaxContentLengthStrategy());
        this.input = input;
        this.output = output;
        this.responseParser = new DefaultHttpResponseParser(input, null, new DefaultHttpResponseFactory(), params);
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

    @Override
    public boolean isStale() {
        return !isOpen(); // TODO: don't know how to reasonably implement this ...
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
    
    public SessionOutputBuffer getOutputBuffer() {
    	return output;
    }
    
	@Override
    public String toString() {
		return "WebdavConnection(" + socket.getPort() + ')';
    }
}