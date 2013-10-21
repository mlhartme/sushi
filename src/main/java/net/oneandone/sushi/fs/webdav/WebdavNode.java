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

import net.oneandone.sushi.fs.*;
import net.oneandone.sushi.fs.webdav.methods.Delete;
import net.oneandone.sushi.fs.webdav.methods.Get;
import net.oneandone.sushi.fs.webdav.methods.Head;
import net.oneandone.sushi.fs.webdav.methods.Method;
import net.oneandone.sushi.fs.webdav.methods.MkCol;
import net.oneandone.sushi.fs.webdav.methods.Move;
import net.oneandone.sushi.fs.webdav.methods.PropFind;
import net.oneandone.sushi.fs.webdav.methods.PropPatch;
import net.oneandone.sushi.fs.webdav.methods.Put;
import net.oneandone.sushi.util.Strings;
import net.oneandone.sushi.util.Util;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.impl.io.ChunkedOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
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

    private final Object tryLock;

    /** @param encodedQuery null or query without initial "?" */
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
        this.tryLock = new Object();
    }

    public URI getURI() {
        return getURI(root.getFilesystem().getScheme());
    }

    public URI getInternalURI() {
        return getURI(root.getFilesystem().getInternalScheme());
    }

    private URI getURI(String scheme) {
        HttpHost host;

        host = root.host;
        try {
            return new URI(scheme, null, host.getHostName(), host.getPort(), "/" + path, getQuery(), null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean equals(Object object) {
        if (object instanceof WebdavNode) {
            if (Util.eq(encodedQuery, ((WebdavNode) object).encodedQuery)) {
                return super.equals(object);
            }
        }
        return false;
    }

    @Override
    public WebdavRoot getRoot() {
        return root;
    }

    @Override
    public WebdavNode getParent() {
        return (WebdavNode) doGetParent();
    }

    @Override
    public WebdavNode join(String ... paths) {
        return (WebdavNode) doJoin(paths);
    }

    @Override
    public WebdavNode join(List<String> paths) {
        return (WebdavNode) doJoin(paths);
    }

    @Override
    public long length() throws LengthException {
        boolean oldTryDir;
        Property property;

        synchronized (tryLock) {
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
            synchronized (tryLock) {
                try {
                    property = getProperty(Name.GETLASTMODIFIED);
                } catch (MovedException e) {
                    tryDir = !tryDir;
                    property = getProperty(Name.GETLASTMODIFIED);
                }
            }
        } catch (IOException e) {
            throw new GetLastModifiedException(this, e);
        }
        try {
            synchronized (FMT) {
                return FMT.parse((String) property.getValue()).getTime();
            }
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
    public String getPermissions() {
        throw unsupported("getPermissions()");
    }

    @Override
    public void setPermissions(String permissions) {
        throw unsupported("setPermissions()");
    }

    @Override
    public UserPrincipal getOwner() {
        throw unsupported("getOwner()");
    }

    @Override
    public void setOwner(UserPrincipal owner) {
        throw unsupported("setOwner()");
    }

    @Override
    public GroupPrincipal getGroup() {
        throw unsupported("getGroup()");
    }

    @Override
    public void setGroup(GroupPrincipal group) {
        throw unsupported("setGroup()");
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getQuery() {
        if (encodedQuery != null) {
            try {
                return new URI("foo://bar/path?" + encodedQuery).getQuery();
            } catch (URISyntaxException e) {
                throw new IllegalStateException();
            }
        } else {
            return null;
        }
    }

    @Override
    public Node deleteFile() throws DeleteException, FileNotFoundException {
        try {
            synchronized (tryLock) {
                tryDir = false;
                new Delete(this).invoke();
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (MovedException e) {
            throw new FileNotFoundException(this, e);
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public Node deleteDirectory() throws DirectoryNotFoundException, DeleteException {
        List<WebdavNode> lst;

        try {
            lst = list();
            if (lst == null) {
                throw new DirectoryNotFoundException(this);
            }
            if (lst.size() > 0) {
                throw new DeleteException(this, "directory is not empty");
            }
            synchronized (tryLock) {
                try {
                    new Delete(this).invoke();
                } catch (MovedException e) {
                    tryDir = !tryDir;
                    new Delete(this).invoke();
                }
            }
        } catch (DirectoryNotFoundException | DeleteException e) {
            throw e;
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public Node deleteTree() throws DeleteException, NodeNotFoundException {
        try {
            synchronized (tryLock) {
                try {
                    new Delete(this).invoke();
                } catch (MovedException e) {
                    tryDir = !tryDir;
                    new Delete(this).invoke();
                }
            }
        } catch (FileNotFoundException e) {
            throw new NodeNotFoundException(this, e);
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public Node move(Node dest, boolean overwrite) throws MoveException {
        if (dest instanceof WebdavNode) {
            return move((WebdavNode) dest, overwrite);
        } else {
            throw new MoveException(this, dest, "cannot move webdav node to none-webdav node");
        }
    }

    public WebdavNode move(WebdavNode dest, boolean overwrite) throws MoveException {
        try {
            synchronized (tryLock) {
                try {
                    dest.tryDir = tryDir;
                    new Move(this, dest, overwrite).invoke();
                } catch (MovedException e) {
                    tryDir = !tryDir;
                    dest.tryDir = tryDir;
                    new Move(this, dest, overwrite).invoke();
                }
            }
		} catch (IOException e) {
			throw new MoveException(this, dest, e.getMessage(), e);
		}
        return dest;
    }

    @Override
    public WebdavNode mkdir() throws MkdirException {
        try {
            synchronized (tryLock) {
                tryDir = true;
                new MkCol(this).invoke();
            }
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
        synchronized (tryLock) {
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
    public InputStream createInputStream() throws CreateInputStreamException, FileNotFoundException {
        synchronized (tryLock) {
            tryDir = false;
            try {
                return new Get(this).invoke();
            } catch (FileNotFoundException e) {
                throw e;
            } catch (IOException e) {
                throw new CreateInputStreamException(this, e);
            }
        }
    }

    public long writeTo(OutputStream dest, long skip) throws FileNotFoundException, WriteToException {
        return writeToImpl(dest, skip);
    }

    @Override
    public OutputStream createOutputStream(boolean append) throws CreateOutputStreamException, FileNotFoundException {
        byte[] add;
        final Put method;
        final WebdavConnection connection;
        OutputStream result;

        try {
            if (isDirectory()) {
                throw new FileNotFoundException(this);
            }
        } catch (ExistsException e) {
            throw new CreateOutputStreamException(this, e);
        }
        try {
            if (append) {
                try {
                    add = readBytes();
                } catch (FileNotFoundException e) {
                    add = null;
                }
            } else {
                add = null;
            }
            synchronized (tryLock) {
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
            }
            return result;
        } catch (IOException e) {
            throw new CreateOutputStreamException(this, e);
        }
    }

    @Override
    public List<WebdavNode> list() throws ListException, DirectoryNotFoundException {
        PropFind method;
        List<WebdavNode> result;
        URI href;

        synchronized (tryLock) {
            try {
                tryDir = true;
                method = new PropFind(this, Name.DISPLAYNAME, 1);
                result = new ArrayList<>();
                for (MultiStatus response : method.invoke()) {
                    try {
                        href = new URI(response.href);
                    } catch (URISyntaxException e) {
                        throw new ListException(this, e);
                    }
                    if (samePath(href)) {
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
            } catch (FileNotFoundException e) {
                throw new DirectoryNotFoundException(this);
            } catch (IOException e) {
                throw new ListException(this, e);
            }
        }
    }

    private boolean samePath(URI uri) {
        String cmp;
        int idx;
        int cl;
        int pl;

        cmp = uri.getPath();
        idx = cmp.indexOf(path);
        if (idx == 1 && cmp.charAt(0) == '/') {
            cl = cmp.length();
            pl = path.length();
            if (cl == 1 + path.length() || cl == 1 + pl + 1 && cmp.charAt(cl - 1) == '/') {
                return true;
            }
        }
        return false;
    }

	private WebdavNode createChild(URI href) {
		String childPath;
		boolean dir;
		WebdavNode result;

        childPath = href.getPath();
		dir = childPath.endsWith("/");
		if (dir) {
		    childPath = childPath.substring(0, childPath.length() - 1);
		}
        childPath = Strings.removeLeft(childPath, "/");
        if (!childPath.startsWith(path)) {
            throw new IllegalStateException();
        }
		result = new WebdavNode(root, childPath, null, dir);
		return result;
	}

    public String getAttribute(String name) throws WebdavException {
    	Property result;
    	Name n;

    	n = new Name(name, Method.DAV);
        try {
            synchronized (tryLock) {
            	try {
        		    result = getPropertyOpt(n);
        	    } catch (MovedException e) {
                    tryDir = !tryDir;
            		result = getPropertyOpt(n);
            	}
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
        synchronized (tryLock) {
            try {
                new PropPatch(this, prop).invoke();
            } catch (MovedException e) {
                tryDir = !tryDir;
                new PropPatch(this, prop).invoke();
            }
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

        synchronized (tryLock) {
            reset = tryDir;
            tryDir = tryTryDir;
            try {
                if (getRoot().getFilesystem().isDav()) {
                    result = doTryDirDav();
                } else {
                    result = doTryDirHttp();
                }
            } catch (MovedException | FileNotFoundException e) {
                tryDir = reset;
                return false;
            } catch (IOException e) {
                tryDir = reset;
                throw new ExistsException(this, e);
            }
            if (!result) {
                tryDir = reset;
            }
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

        synchronized (tryLock) {
            builder = new StringBuilder(path.length() + 10);
            builder.append('/');
            if (!path.isEmpty()) {
                try {
                    builder.append(new URI(null, null, path, null).getRawPath());
                } catch (URISyntaxException e) {
                    throw new IllegalStateException();
                }
                if (tryDir) {
                    builder.append('/');
                }
            }
            if (encodedQuery != null) {
                builder.append('?');
                builder.append(encodedQuery);
            }
        }
        return builder.toString();
    }
}
