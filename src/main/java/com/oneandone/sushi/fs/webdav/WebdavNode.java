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

import com.oneandone.sushi.fs.DeleteException;
import com.oneandone.sushi.fs.ExistsException;
import com.oneandone.sushi.fs.GetLastModifiedException;
import com.oneandone.sushi.fs.LengthException;
import com.oneandone.sushi.fs.ListException;
import com.oneandone.sushi.fs.MkdirException;
import com.oneandone.sushi.fs.MoveException;
import com.oneandone.sushi.fs.Node;
import com.oneandone.sushi.fs.SetLastModifiedException;
import com.oneandone.sushi.fs.webdav.methods.Delete;
import com.oneandone.sushi.fs.webdav.methods.Get;
import com.oneandone.sushi.fs.webdav.methods.Head;
import com.oneandone.sushi.fs.webdav.methods.Method;
import com.oneandone.sushi.fs.webdav.methods.MkCol;
import com.oneandone.sushi.fs.webdav.methods.Move;
import com.oneandone.sushi.fs.webdav.methods.PropFind;
import com.oneandone.sushi.fs.webdav.methods.PropPatch;
import com.oneandone.sushi.fs.webdav.methods.Put;
import com.oneandone.sushi.util.Strings;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.impl.io.ChunkedOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WebdavNode extends Node {
	private final WebdavRoot root;

	/** 
     * Never starts with a slash. 
     * Without type - never with tailing /. With special characters - will be encoded in http requests  
     */
    private final String path;

    /**
     * Null or never starts with ?
     */
    private final String encodedQuery;

    private boolean tryDir;
    
    public WebdavNode(WebdavRoot root, String path, String encodedQuery, boolean tryDir) {
        if (path.startsWith("/")) {
            throw new IllegalArgumentException(path);
        }
        if (encodedQuery != null && encodedQuery.startsWith("?")) {
            throw new IllegalArgumentException(path);
        }
        this.root = root;
        this.path = path;
        this.encodedQuery = encodedQuery;
        this.tryDir = tryDir;
    }

    public URI getURI() {
        HttpHost host;

        host = root.host;
        try {
            return new URI(host.getSchemeName(), null, host.getHostName(), host.getPort(), "/" + path, getQuery(), null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public WebdavRoot getRoot() {
        return root;
    }

    @Override
    public long length() throws LengthException {
        boolean oldTryDir;
        Property property;

        oldTryDir = tryDir;
        try {
            tryDir = false;
            property = getProperty(Name.GETCONTENTLENGTH);
        } catch (IOException e) {
            tryDir = oldTryDir;
            throw new LengthException(this, e);
        }
        return Long.parseLong((String) property.getValue());
    }

    private static final SimpleDateFormat FMT;        
    
    static {
        Calendar calendar;

        calendar = Calendar.getInstance();
        calendar.set(2000, Calendar.JANUARY, 1, 0, 0);
        FMT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        FMT.setTimeZone(TimeZone.getTimeZone("GMT"));
        FMT.set2DigitYearStart(calendar.getTime());
    }
    
    @Override
    public long getLastModified() throws GetLastModifiedException {
        Property property;
        
        try {
        	try {
        		property = getProperty(Name.GETLASTMODIFIED);
        	} catch (MovedException e) {
                tryDir = !tryDir;
        		property = getProperty(Name.GETLASTMODIFIED);
        	}
        } catch (IOException e) {
            throw new GetLastModifiedException(this, e);
        }
        try {
            return FMT.parse((String) property.getValue()).getTime();
        } catch (ParseException e) {
            throw new GetLastModifiedException(this, e);
        }
    }

    @Override
    public void setLastModified(long millis) throws SetLastModifiedException {
        // no allowed by webdav standard
        throw new SetLastModifiedException(this);
    }
    
    @Override 
    public int getMode() {
        throw unsupported("getMode()");
    }

    @Override
    public void setMode(int mode) {
        throw unsupported("setMode()");
    }
    
    @Override 
    public int getUid() {
        throw unsupported("getUid()");
    }

    @Override
    public void setUid(int uid) {
        throw unsupported("setUid()");
    }

    @Override 
    public int getGid() {
        throw unsupported("getGid()");
    }

    @Override
    public void setGid(int gid) {
        throw unsupported("setGid()");
    }

    private boolean sameUrl(String href) {
        String str;
        StringBuilder builder;
        
        str = path;
        if (href.startsWith("/")) {
            href = href.substring(1);
        }
        builder = new StringBuilder();
        for (String segment : getRoot().getFilesystem().split(href)) {
            if (builder.length() > 0) {
                builder.append('/');
            }
            try {
                builder.append(URLDecoder.decode(segment, WebdavFilesystem.ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(href, e);
            }
        }
        href = builder.toString();
        if (href.endsWith("/") && !str.endsWith("/")) {
            str += "/";
        }
        return href.equals(str);
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getQuery() {
        if (encodedQuery != null) {
            try {
                return URLDecoder.decode(encodedQuery, WebdavFilesystem.ENCODING);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return null;
        }
    }

    @Override
    public Node delete() throws DeleteException {
        try {
        	try {
        		new Delete(this).invoke();
        	} catch (MovedException e) {
                tryDir = !tryDir;
        		new Delete(this).invoke();
        	}
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public Node move(Node dest) throws MoveException {
        if (dest instanceof WebdavNode) {
            return move((WebdavNode) dest);
        } else {
            throw new MoveException(this, dest, "cannot move webdav node to none-webdav node");
        }
    }

    public WebdavNode move(WebdavNode dest) throws MoveException {
        try {
        	try {
                dest.tryDir = tryDir;
        		new Move(this, dest.getURI()).invoke();
        	} catch (MovedException e) {
                tryDir = !tryDir;
                dest.tryDir = tryDir;
        		new Move(this, dest.getURI()).invoke();
        	}
		} catch (IOException e) {
			throw new MoveException(this, dest, e.getMessage(), e);
		}
        return dest;
    }

    @Override
    public WebdavNode mkdir() throws MkdirException {
        try {
            tryDir = true;
            new MkCol(this).invoke();
        } catch (IOException e) {
            throw new MkdirException(this, e);
        }
        return this;
    }

    @Override
    public void mklink(String target) {
        throw unsupported("mklink()");
    }

    @Override
    public String readLink() {
        throw unsupported("readLink()");
    }

    @Override
    public boolean exists() throws ExistsException {
        try {
            new Head(this).invoke();
            return true;
        } catch (StatusException e) {
            switch (e.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_MOVED_PERMANENTLY:
                    tryDir = !tryDir;
                    return true;
                case HttpStatus.SC_NOT_FOUND:
                    return false;
                default:
                    throw new ExistsException(this, e);
            }
        } catch (IOException e) {
            throw new ExistsException(this, e);
        }
    }

    @Override
    public boolean isFile() throws ExistsException {
        return tryDir(false);
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        return tryDir(true);
    }
    
    @Override
    public boolean isLink() {
    	return false;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        tryDir = false;
        return new Get(this).invoke();
    }

    @Override
    public OutputStream createOutputStream(boolean append) throws IOException {
        byte[] add;
        final Put method;
        final WebdavConnection connection;
        OutputStream result;

        if (append) {
            try {
                add = readBytes();
            } catch (FileNotFoundException e) {
                add = null;
            }
        } else {
            add = null;
        }
        tryDir = false;
        method = new Put(this);
        connection = method.request();
        result = new ChunkedOutputStream(connection.getOutputBuffer()) {
            private boolean closed = false;
            @Override
            public void close() throws IOException {
                if (closed) {
                    return;
                }
                closed = true;
                super.close();
                method.response(connection);
            }
        };
        if (add != null) {
            result.write(add);
        }
        return result;
    }

    @Override
    public List<Node> list() throws ListException {
        PropFind method;
        List<Node> result;
        String href;
        
        try {
            tryDir = true;
            method = new PropFind(this, Name.DISPLAYNAME, 1);
            result = new ArrayList<Node>();
            for (MultiStatus response : method.invoke()) {
            	href = response.href;
                if (sameUrl(href)) {
                    // ignore "."
                } else {
                	result.add(createChild(href));
                }
            }
            return result;
        } catch (StatusException e) {
            if (e.getStatusLine().getStatusCode() == 400) {
                return null; // this is a file
            }
            throw new ListException(this, e);
        } catch (MovedException e) {
            tryDir = false;
            return null; // this is a file
        } catch (IOException e) {
            throw new ListException(this, e);
        }
    }

	private WebdavNode createChild(String href) throws UnsupportedEncodingException {
		int i;
		boolean dir;
		WebdavNode result;
		
		dir = href.endsWith("/");
		if (dir) {
		    href = href.substring(0, href.length() - 1);
		}
		i = href.lastIndexOf("/");
		href = href.substring(i + 1); // ok for i == -1
		href = URLDecoder.decode(href, WebdavFilesystem.ENCODING);
		result = new WebdavNode(root, path + '/' + href, null, dir);
		result.setBase(getBase());
		return result;
	}

    public String getAttribute(String name) throws WebdavException {
    	Property result;
    	Name n;

    	n = new Name(name, Method.DAV);
        try {
        	try {
        		result = getPropertyOpt(n);
        	} catch (MovedException e) {
                tryDir = !tryDir;
        		result = getPropertyOpt(n);
        	}
        	return result == null ? null : (String) result.getValue();
		} catch (IOException e) {
			throw new WebdavException(this, e);
		}
    }

    public void setAttribute(String name, String value) throws WebdavException {
        try {
        	setProperty(new Name(name, Method.DAV), value);
		} catch (IOException e) {
			throw new WebdavException(this, e);
		}
    }

    private void setProperty(Name name, String value) throws IOException {
    	Property prop;
    	
        prop = new Property(name, value);
       	try {
       		new PropPatch(this, prop).invoke();
       	} catch (MovedException e) {
            tryDir = !tryDir;
       		new PropPatch(this, prop).invoke();
      	}
    }

    /** @return never null */
    private Property getProperty(Name name) throws IOException {
    	Property result;
        
        result = getPropertyOpt(name);
        if (result == null) {
            throw new IllegalStateException();
        }
        return result;
    }

    private Property getPropertyOpt(Name name) throws IOException {
        PropFind method;
        List<MultiStatus> response;
        
        method = new PropFind(this, name, 0);
        response = method.invoke();
        return MultiStatus.lookupOne(response, name).property;
    }

    private boolean tryDir(boolean tryTryDir) throws ExistsException {
        boolean reset;
        boolean result;

        reset = tryDir;
        tryDir = tryTryDir;
        try {
            if (getRoot().getFilesystem() instanceof HttpFilesystem) {
                result = doTryDirHttp();
            } else {
                result = doTryDirDav();
            }
        } catch (MovedException e) {
            tryDir = reset;
            return false;
        } catch (FileNotFoundException e) {
            tryDir = reset;
            return false;
        } catch (IOException e) {
            tryDir = reset;
            throw new ExistsException(this, e);
        }
        if (!result) {
            tryDir = reset;
        }
        return result;
    }

    private boolean doTryDirDav() throws IOException {
        Property property;
        org.w3c.dom.Node node;

        property = getProperty(Name.RESOURCETYPE);
        node = (org.w3c.dom.Node) property.getValue();
        if (node == null) {
            return tryDir == false;
        }
        return tryDir == "collection".equals(node.getLocalName());
    }

    private boolean doTryDirHttp() throws IOException {
        try {
            new Head(this).invoke();
            return true;
        } catch (StatusException e2) {
            switch (e2.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_MOVED_PERMANENTLY:
                    return false;
                case HttpStatus.SC_NOT_FOUND:
                    return false;
                default:
                    throw e2;
            }
        }
    }

    //--
    
    /** see http://tools.ietf.org/html/rfc2616#section-5.1.2 */
    public String getAbsPath() {
        StringBuilder builder;

        builder = new StringBuilder(path.length() + 10);
        for (String segment : Strings.split("/", path)) {
            builder.append('/');
            try {
                builder.append(URLEncoder.encode(segment, WebdavFilesystem.ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        if (tryDir) {
            builder.append('/');
        }
        if (encodedQuery != null) {
            builder.append('?');
            builder.append(encodedQuery);
        }
        return builder.toString();
    }
}
