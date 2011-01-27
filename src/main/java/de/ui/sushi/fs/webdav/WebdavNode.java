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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.http.HttpStatus;
import org.apache.http.impl.io.ChunkedOutputStream;
import de.ui.sushi.fs.DeleteException;
import de.ui.sushi.fs.ExistsException;
import de.ui.sushi.fs.GetLastModifiedException;
import de.ui.sushi.fs.LengthException;
import de.ui.sushi.fs.ListException;
import de.ui.sushi.fs.MkdirException;
import de.ui.sushi.fs.MoveException;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.SetLastModifiedException;
import de.ui.sushi.fs.webdav.methods.DeleteMethod;
import de.ui.sushi.fs.webdav.methods.GetMethod;
import de.ui.sushi.fs.webdav.methods.HeadMethod;
import de.ui.sushi.fs.webdav.methods.MkColMethod;
import de.ui.sushi.fs.webdav.methods.MoveMethod;
import de.ui.sushi.fs.webdav.methods.PropFindMethod;
import de.ui.sushi.fs.webdav.methods.PropPatchMethod;
import de.ui.sushi.fs.webdav.methods.PutMethod;
import de.ui.sushi.fs.webdav.methods.WebdavMethod;

public class WebdavNode extends Node {
	private final WebdavRoot root;

	/** 
     * Never starts with a slash. 
     * Without type - never with tailing /. With special characters - will be encoded in http requests  
     */
    private final String path;
    private boolean tryDir;
    
    public WebdavNode(WebdavRoot root, String path, boolean tryDir) {
        if (path.startsWith("/")) {
            throw new IllegalArgumentException(path);
        }
        this.root = root;
        this.path = path;
        this.tryDir = tryDir;
    }

    @Override
    public WebdavRoot getRoot() {
        return root;
    }

    @Override
    public long length() throws LengthException {
        Property property;
        
        try {
            property = getProperty(path, Name.GETCONTENTLENGTH);
        } catch (IOException e) {
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
        		property = getProperty(path(tryDir), Name.GETLASTMODIFIED);
        	} catch (MovedException e) {
        		property = getProperty(path(!tryDir), Name.GETLASTMODIFIED);
        		tryDir = !tryDir;
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

    @Override
    public Node delete() throws DeleteException {
        try {
        	try {
        		new DeleteMethod(root, path).invoke();
        	} catch (MovedException e) {
        		new DeleteMethod(root, path + '/').invoke();
        	}
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public Node move(Node dest) throws MoveException {
        String after;

        after = root.toUrl(((WebdavNode) dest).path);
        try {
        	try {
        		new MoveMethod(root, path(tryDir), path(after, tryDir)).invoke();
        	} catch (MovedException e) {
        		new MoveMethod(root, path(!tryDir), path(after, !tryDir)).invoke();
        		tryDir = !tryDir;
        	}
		} catch (IOException e) {
			throw new MoveException(this, dest, e.getMessage(), e);
		}
        return dest;
    }

    @Override
    public WebdavNode mkdir() throws MkdirException {
        try {
            new MkColMethod(root, path + "/").invoke();
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
        throw unsupported("readLinkT()");
    }

    @Override
    public boolean exists() throws ExistsException {
        try {
            new HeadMethod(getRoot(), path(path, tryDir)).invoke();
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
        return Boolean.FALSE.equals(getType());
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        return Boolean.TRUE.equals(getType());
    }
    
    @Override
    public boolean isLink() {
    	return false;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return new GetMethod(root, path).invoke();
    }

    @Override
    public OutputStream createOutputStream(boolean append) throws IOException {
        byte[] add;
        final PutMethod method;
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
        method = new PutMethod(root, path);
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
        PropFindMethod method;
        List<Node> result;
        String href;
        
        try {
            method = new PropFindMethod(root, path + "/", Name.DISPLAYNAME, 1);
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
		result = new WebdavNode(root, path + '/' + href, dir);
		result.setBase(getBase());
		return result;
	}

    public String getAttribute(String name) throws WebdavException {
    	Property result;
    	Name n;

    	n = new Name(name, WebdavMethod.DAV);
        try {
        	try {
        		result = getPropertyOpt(path(tryDir), n);
        	} catch (MovedException e) {
        		result = getPropertyOpt(path(!tryDir), n);
        		tryDir = !tryDir;
        	}
        	return result == null ? null : (String) result.getValue();
		} catch (IOException e) {
			throw new WebdavException(this, e);
		}
    }

    public void setAttribute(String name, String value) throws WebdavException {
        try {
        	setProperty(new Name(name, WebdavMethod.DAV), value);
		} catch (IOException e) {
			throw new WebdavException(this, e);
		}
    }

    private void setProperty(Name name, String value) throws IOException {
    	Property prop;
    	
        prop = new Property(name, value);
       	try {
       		new PropPatchMethod(root, path(tryDir), prop).invoke();
       	} catch (MovedException e) {
       		new PropPatchMethod(root, path(!tryDir), prop).invoke();
       		tryDir = !tryDir;
      	}
    }

    /** @return never null */
    private Property getProperty(String path, Name name) throws IOException {
    	Property result;
        
        result = getPropertyOpt(path, name);
        if (result == null) {
            throw new IllegalStateException();
        }
        return result;
    }

    private Property getPropertyOpt(String path, Name name) throws IOException {
        PropFindMethod method;
        List<MultiStatus> response;
        
        method = new PropFindMethod(root, path, name, 0);
        response = method.invoke();
        return MultiStatus.lookupOne(response, name).property;
    }

    /** @return null if not existing, true for dir, false for file */
    private Boolean getType() throws ExistsException {
    	boolean result;
    	
        try {
            try {
                return doGetType(path(tryDir), tryDir);
            } catch (FileNotFoundException fse) {
                return null;
            } catch (MovedException e) {
            	// TODO: omit this call?
            	result = doGetType(path(!tryDir), !tryDir);
            	tryDir = !tryDir;
            	return result;
            }
        } catch (IOException e) {
            throw new ExistsException(this, e);
        }
    }

    private Boolean doGetType(String path, boolean dirPath) throws IOException {
        Property property;
        org.w3c.dom.Node node;
        int code;

        try {
            property = getProperty(path, Name.RESOURCETYPE);
        } catch (StatusException e) {
            code = e.getStatusLine().getStatusCode();
            if (code == HttpStatus.SC_METHOD_NOT_ALLOWED || code == HttpStatus.SC_UNAUTHORIZED /* returned by Nexus 1.7.0 */) {
                try {
                    new HeadMethod(getRoot(), path).invoke();
                    return dirPath;
                } catch (StatusException e2) {
                    switch (e2.getStatusLine().getStatusCode()) {
                        case HttpStatus.SC_MOVED_PERMANENTLY:
                            throw new MovedException();
                        case HttpStatus.SC_NOT_FOUND:
                            return null;
                        default:
                            throw e2;
                    }
                }
            } else {
                throw e;
            }
        }
        node = (org.w3c.dom.Node) property.getValue();
        return node == null ? false : node.getLocalName().equals("collection");
    }

    //--
    
    private String path(boolean dir) {
    	return path(path, dir);
    }

    private static String path(String path, boolean dir) {
    	return dir ? path + "/" : path;
    }
    
    public String getUrl() {
        return root.getFilesystem().getScheme() + ":" + root.getId() + getPath();
    }
}
