/*
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

import net.oneandone.sushi.fs.CopyFileFromException;
import net.oneandone.sushi.fs.CopyFileToException;
import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.MoveException;
import net.oneandone.sushi.fs.NewDirectoryOutputStreamException;
import net.oneandone.sushi.fs.NewInputStreamException;
import net.oneandone.sushi.fs.NewOutputStreamException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.SetLastModifiedException;
import net.oneandone.sushi.fs.SizeException;
import net.oneandone.sushi.fs.http.model.Body;
import net.oneandone.sushi.fs.http.model.Header;
import net.oneandone.sushi.fs.http.model.Method;
import net.oneandone.sushi.fs.http.model.MultiStatus;
import net.oneandone.sushi.fs.http.model.Name;
import net.oneandone.sushi.fs.http.model.Property;
import net.oneandone.sushi.fs.http.model.ProtocolException;
import net.oneandone.sushi.fs.http.model.Request;
import net.oneandone.sushi.fs.http.model.Response;
import net.oneandone.sushi.fs.http.model.StatusCode;
import net.oneandone.sushi.util.Strings;
import net.oneandone.sushi.util.Util;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
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

public class HttpNode extends Node<HttpNode> {
	private final HttpRoot root;

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

    /** true if this node is know to accep webdav commands, false if it's know to not accept them. Null if unknown */
    private Boolean isDav;

    /** @param encodedQuery null or query without initial "?" */
    public HttpNode(HttpRoot root, String path, String encodedQuery, boolean tryDir, Boolean isDav) {
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
        this.isDav = isDav;
    }

    @Override
    public URI getUri() {
        try {
            return new URI(root.getFilesystem().getScheme(), null, root.getHostname(), root.getPort(), "/" + path, getQuery(), null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public URI getUriWithUserInfo() {
        try {
            return new URI(root.getFilesystem().getScheme(), root.getUserInfo(), root.getHostname(), root.getPort(), "/" + path, getQuery(), null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean equals(Object object) {
        if (object instanceof HttpNode) {
            if (Util.eq(encodedQuery, ((HttpNode) object).encodedQuery)) {
                return super.equals(object);
            }
        }
        return false;
    }

    @Override
    public HttpRoot getRoot() {
        return root;
    }

    @Override
    public long size() throws SizeException {
        String result;

        try {
            if (isDav == null) {
                try {
                    result = davSize();
                    isDav = true;
                } catch (StatusException e) {
                    if (e.getStatusLine().code == StatusCode.METHOD_NOT_ALLOWED) {
                        isDav = false;
                        result = headSize();
                    } else {
                        throw e;
                    }
                } catch (FileNotFoundException e) {
                    // isDav remains null
                    result = headSize();
                }
            } else if (isDav) {
                result = davSize();
            } else {
                result = headSize();
            }
        } catch (IOException e) {
            throw new SizeException(this, e);
        }
        try {
            return Long.parseLong(result);
        } catch (NumberFormatException e) {
            throw new SizeException(this, e);
        }
    }

    public String davSize() throws IOException {
        boolean oldTryDir;
        Property property;

        synchronized (tryLock) {
            oldTryDir = tryDir;
            try {
                tryDir = false;
                property = getProperty(Name.GETCONTENTLENGTH);
            } catch (IOException e) {
                tryDir = oldTryDir;
                throw e;
            }
            return (String) property.getValue();
        }
    }

    public String headSize() throws IOException {
        boolean oldTryDir;
        String result;

        synchronized (tryLock) {
            oldTryDir = tryDir;
            try {
                tryDir = false;
                result = Method.head(this, Header.CONTENT_LENGTH);
            } catch (IOException e) {
                tryDir = oldTryDir;
                throw e;
            }
        }
        if (result == null) {
            throw new ProtocolException("head request did not return content length");
        }
        return result;
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
        String result;

        try {
            if (isDav == null) {
                try {
                    result = davGetLastModified();
                    isDav = true;
                } catch (StatusException e) {
                    if (e.getStatusLine().code == StatusCode.METHOD_NOT_ALLOWED) {
                        isDav = false;
                        result = headGetLastModified();
                    } else {
                        throw e;
                    }
                } catch (FileNotFoundException e) {
                    // isDav remains null
                    result = headGetLastModified();
                }
            } else if (isDav) {
                result = davGetLastModified();
            } else {
                result = headGetLastModified();
            }
        } catch (IOException e) {
            throw new GetLastModifiedException(this, e);
        }
        try {
            synchronized (FMT) {
                return FMT.parse(result).getTime();
            }
        } catch (ParseException e) {
            throw new GetLastModifiedException(this, e);
        }
    }

    public String davGetLastModified() throws IOException {
        synchronized (tryLock) {
            try {
                return (String) getProperty(Name.GETLASTMODIFIED).getValue();
            } catch (MovedPermanentlyException e) {
                tryDir = !tryDir;
                return (String) getProperty(Name.GETLASTMODIFIED).getValue();
            }
        }
    }

    public String headGetLastModified() throws IOException {
        synchronized (tryLock) {
            try {
                return doHeadGetLastModified();
            } catch (MovedPermanentlyException e) {
                tryDir = !tryDir;
                return doHeadGetLastModified();
            }
        }
    }

    private String doHeadGetLastModified() throws IOException {
        String result;
        result = Method.head(this, "Last-Modified");
        if (result == null) {
            throw new ProtocolException("head request did not return last-modified header");
        }
        return result;
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
    public HttpNode deleteFile() throws DeleteException, FileNotFoundException {
        try {
            synchronized (tryLock) {
                tryDir = false;
                Method.delete(this);
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (MovedPermanentlyException e) {
            throw new FileNotFoundException(this, e);
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public HttpNode deleteDirectory() throws DirectoryNotFoundException, DeleteException {
        List<HttpNode> lst;

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
                    Method.delete(this);
                } catch (MovedPermanentlyException e) {
                    tryDir = !tryDir;
                    Method.delete(this);
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
    public HttpNode deleteTree() throws DeleteException, NodeNotFoundException {
        try {
            synchronized (tryLock) {
                try {
                    Method.delete(this);
                } catch (MovedPermanentlyException e) {
                    tryDir = !tryDir;
                    Method.delete(this);
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
    public Node move(Node dest, boolean overwrite) throws FileNotFoundException, MoveException {
        if (dest instanceof HttpNode) {
            return move((HttpNode) dest, overwrite);
        } else {
            throw new MoveException(this, dest, "cannot move http node to none-http node");
        }
    }

    public HttpNode move(HttpNode dest, boolean overwrite) throws FileNotFoundException, MoveException {
        try {
            synchronized (tryLock) {
                try {
                    dest.tryDir = tryDir;
                    Method.move(this, dest, overwrite);
                } catch (MovedPermanentlyException e) {
                    tryDir = !tryDir;
                    dest.tryDir = tryDir;
                    Method.move(this, dest, overwrite);
                }
            }
        } catch (FileNotFoundException e) {
            throw e;
		} catch (IOException e) {
			throw new MoveException(this, dest, e.getMessage(), e);
		}
        return dest;
    }

    @Override
    public HttpNode mkdir() throws MkdirException {
        try {
            synchronized (tryLock) {
                tryDir = true;
                Method.mkcol(this);
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
                Method.head(this, null);
                return true;
            } catch (StatusException e) {
                switch (e.getStatusLine().code) {
                    case StatusCode.MOVED_PERMANENTLY:
                        tryDir = !tryDir;
                        return true;
                    case StatusCode.NOT_FOUND:
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
        return isNode(false);
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        return isNode(true);
    }

    @Override
    public boolean isLink() {
    	return false;
    }

    @Override
    public InputStream newInputStream() throws NewInputStreamException, FileNotFoundException {
        synchronized (tryLock) {
            tryDir = false;
            try {
                return Method.get(this);
            } catch (FileNotFoundException e) {
                throw e;
            } catch (IOException e) {
                throw new NewInputStreamException(this, e);
            }
        }
    }

    public long copyFileTo(OutputStream dest, long skip) throws FileNotFoundException, CopyFileToException {
        return copyFileToImpl(dest, skip);
    }

    public void copyFileFrom(InputStream dest) throws FileNotFoundException, CopyFileFromException {
        copyFileFromImpl(dest);
    }

    @Override
    public OutputStream newOutputStream(boolean append) throws NewOutputStreamException {
        byte[] add;
        OutputStream result;

        try {
            if (isDirectory()) {
                throw new NewDirectoryOutputStreamException(this);
            }
        } catch (ExistsException e) {
            throw new NewOutputStreamException(this, e);
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
                result = Method.put(this);
                if (add != null) {
                    result.write(add);
                }
            }
            return result;
        } catch (IOException e) {
            throw new NewOutputStreamException(this, e);
        }
    }

    @Override
    public List<HttpNode> list() throws ListException, DirectoryNotFoundException {
        List<HttpNode> result;
        URI href;

        synchronized (tryLock) {
            try {
                tryDir = true;
                result = new ArrayList<>();
                for (MultiStatus response : Method.propfind(this, Name.DISPLAYNAME, 1)) {
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
                if (e.getStatusLine().code == StatusCode.BAD_REQUEST) {
                    return null; // this is a file
                }
                throw new ListException(this, e);
            } catch (MovedPermanentlyException e) {
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

	private HttpNode createChild(URI href) {
		String childPath;
		boolean dir;
		HttpNode result;

        childPath = href.getPath();
		dir = childPath.endsWith("/");
		if (dir) {
		    childPath = childPath.substring(0, childPath.length() - 1);
		}
        childPath = Strings.removeLeft(childPath, "/");
        if (!childPath.startsWith(path)) {
            throw new IllegalStateException();
        }
		result = new HttpNode(root, childPath, null, dir, isDav);
		return result;
	}

    public String getAttribute(String name) throws HttpException {
    	Property result;
    	Name n;

    	n = new Name(name, Method.DAV);
        try {
            synchronized (tryLock) {
            	try {
        		    result = getPropertyOpt(n);
        	    } catch (MovedPermanentlyException e) {
                    tryDir = !tryDir;
            		result = getPropertyOpt(n);
            	}
            }
        	return result == null ? null : (String) result.getValue();
		} catch (IOException e) {
			throw new HttpException(this, e);
		}
    }

    public void setAttribute(String name, String value) throws HttpException {
        try {
        	setProperty(new Name(name, Method.DAV), value);
		} catch (IOException e) {
			throw new HttpException(this, e);
		}
    }

    private void setProperty(Name name, String value) throws IOException {
    	Property prop;

        prop = new Property(name, value);
        synchronized (tryLock) {
            try {
                Method.proppatch(this, prop);
            } catch (MovedPermanentlyException e) {
                tryDir = !tryDir;
                Method.proppatch(this, prop);
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
        List<MultiStatus> response;

        response = Method.propfind(this, name, 0);
        return MultiStatus.lookupOne(response, name).property;
    }

    private boolean isNode(boolean directory) throws ExistsException {
        boolean reset;
        boolean result;

        synchronized (tryLock) {
            reset = tryDir;
            tryDir = directory;
            try {
                if (isDav == null) {
                    try {
                        result = davIsNode();
                        isDav = true;
                    } catch (StatusException e) {
                        if (e.getStatusLine().code == StatusCode.METHOD_NOT_ALLOWED
                                || e.getStatusLine().code == StatusCode.OK) {
                            isDav = false;
                            result = headIsNode();
                        } else {
                            throw e;
                        }
                    } catch (FileNotFoundException e) {
                        // isDav remains null null;
                        result = headIsNode();
                    }
                } else if (isDav) {
                    result = davIsNode();
                } else {
                    result = headIsNode();
                }
            } catch (MovedPermanentlyException | FileNotFoundException e) {
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

    private boolean davIsNode() throws IOException {
        Property property;
        org.w3c.dom.Node node;

        property = getProperty(Name.RESOURCETYPE);
        node = (org.w3c.dom.Node) property.getValue();
        if (node == null) {
            return !tryDir;
        }
        return tryDir == "collection".equals(node.getLocalName());
    }

    private boolean headIsNode() throws IOException {
        try {
            Method.head(this, null);
            return true;
        } catch (StatusException e) {
            switch (e.getStatusLine().code) {
                case StatusCode.MOVED_PERMANENTLY:
                    return false;
                case StatusCode.NOT_FOUND:
                    return false;
                default:
                    throw e;
            }
        }
    }

    //--

    /** see http://tools.ietf.org/html/rfc2616#section-5.1.2 */
    public String getRequestPath() {
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


    //-- REST methods

    public void put(String str) throws IOException {
        put(getWorld().getSettings().bytes(str));
    }

    public void put(byte ... bytes) throws IOException {
        try (OutputStream dest = Method.put(this)) {
            dest.write(bytes);
        }
    }

    public String post(String str) throws IOException {
        byte[] result;

        result = post(getWorld().getSettings().bytes(str));
        return getWorld().getSettings().string(result);
    }
    public byte[] post(byte[] body) throws IOException {
        return post(new Body(null, null, body.length, new ByteArrayInputStream(body), false));
    }
    public byte[] post(InputStream body) throws IOException {
        return post(new Body(null, null, -1, body, false));
    }
    public byte[] post(Body body) throws IOException {
        return Method.post(this, body);
    }
    public InputStream postStream(Body body) throws IOException {
        Request post;
        Response response;

        post = new Request("POST", this);
        post.bodyHeader(body);
        response = post.responseHeader(post.open(body));
        if (response.getStatusLine().code == StatusCode.OK) {
            return new FilterInputStream(response.getBody().content) {
                private boolean freed = false;

                @Override
                public void close() throws IOException {
                    if (!freed) {
                        freed = true;
                        post.free(response);
                    }
                    super.close();
                }
            };
        } else {
            post.free(response);
            switch (response.getStatusLine().code) {
                case StatusCode.MOVED_TEMPORARILY:
                    throw new MovedTemporarilyException(response.getHeaderList().getFirstValue("Location"));
                case StatusCode.NOT_FOUND:
                case StatusCode.GONE:
                case StatusCode.MOVED_PERMANENTLY:
                    throw new FileNotFoundException(this);
                default:
                    throw StatusException.forResponse(response);
            }
        }
    }



    public String patch(String str) throws IOException {
        byte[] result;

        result = patch(getWorld().getSettings().bytes(str));
        return getWorld().getSettings().string(result);

    }
    public byte[] patch(byte[] body) throws IOException {
        return patch(new Body(null, null, body.length, new ByteArrayInputStream(body), false));
    }
    public byte[] patch(InputStream body) throws IOException {
        return patch(new Body(null, null, -1, body, false));
    }
    public byte[] patch(Body body) throws IOException {
        return Method.patch(this, body);
    }

}
