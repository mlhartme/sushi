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
package net.oneandone.sushi.fs.zip;

import net.oneandone.sushi.fs.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Represents an entry in a zip or jar file. Read-only */
public class ZipNode extends Node {
    private final ZipRoot root;
    private final String path;

    public ZipNode(ZipRoot root, String path) {
        super();
        this.root = root;
        this.path = path;
    }

    @Override
    public ZipRoot getRoot() {
        return root;
    }

    @Override
    public ZipNode getParent() {
        return (ZipNode) doGetParent();
    }

    @Override
    public ZipNode join(String ... paths) {
        return (ZipNode) doJoin(paths);
    }

    @Override
    public ZipNode join(List<String> paths) {
        return (ZipNode) doJoin(paths);
    }

    @Override
    public long length() throws LengthException {
        ZipEntry entry;

        entry = root.getZip().getEntry(path);
        if (entry == null) {
            throw new LengthException(this, new IOException("file expected"));
        }
        return entry.getSize();
    }

    @Override
    public long getLastModified() {
        return root.getLastModified();
    }

    @Override
    public void setLastModified(long millis) throws SetLastModifiedException {
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
    public Node deleteFile() throws DeleteException {
        throw new DeleteException(this);
    }

    @Override
    public Node deleteDirectory() throws DeleteException {
        throw new DeleteException(this);
    }

    @Override
    public Node deleteTree() throws DeleteException {
        throw new DeleteException(this);
    }

    @Override
    public Node move(Node dest, boolean overwrite) throws MoveException {
    	throw new MoveException(this, dest, "ZipNode cannot be moved");
    }

    @Override
    public Node mkdir() throws MkdirException {
        throw new MkdirException(this);
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
    public boolean exists() {
        return root.getZip().getEntry(path)  != null || isDirectory();
    }

    @Override
    public boolean isFile() {
        ZipEntry entry;
        InputStream in;

        entry = root.getZip().getEntry(path);
        if (entry == null) {
            return false;
        }
        if (entry.getSize() > 0) {
            return true;
        }
        try {
            in = root.getZip().getInputStream(entry);
        } catch (IOException e) {
            return true;
        }
        if (in == null) {
            return false;
        } else {
            try {
                in.close();
            } catch (IOException e) {
                // fall through
            }
            return true;
        }
    }

    @Override
    public boolean isLink() {
    	return false;
    }

    @Override
    public boolean isDirectory() {
        ZipFile zip;

        if (path.isEmpty()) {
            return true;
        }
        zip = root.getZip();
        if (zip.getEntry(path + "/") != null) {
            return true;
        }
        // also contains implicit directories
        return root.list(path).size() > 0;
    }

    @Override
    public InputStream createInputStream() throws FileNotFoundException, CreateInputStreamException {
        ZipFile zip;
        ZipEntry entry;

        zip = root.getZip();
        entry = zip.getEntry(path);
        if (entry == null) {
            throw new FileNotFoundException(this);
        }
        try {
            return zip.getInputStream(entry);
        } catch (IOException e) {
            throw new CreateInputStreamException(this, e);
        }
    }

    public long writeTo(OutputStream dest, long skip) throws WriteToException, FileNotFoundException {
        return writeToImpl(dest, skip);
    }

    @Override
    public OutputStream createOutputStream(boolean append) {
        throw unsupported("createOutputStream(" + append + ")");
    }

    @Override
    public List<ZipNode> list() throws DirectoryNotFoundException, ListException {
        List<String> paths;
        List<ZipNode> result;

        if (isFile()) {
            return null;
        }
        paths = root.list(path);
        if (paths.size() == 0 && root.getZip().getEntry(path + "/") == null) {
            throw new DirectoryNotFoundException(this);
        }
        result = new ArrayList<ZipNode>();
        for (String path : paths) {
            result.add(root.node(path, null));
        }
        return result;
    }

    @Override
    public String getPath() {
        return path;
    }
}
