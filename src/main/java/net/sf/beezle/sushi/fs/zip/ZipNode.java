/**
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
package net.sf.beezle.sushi.fs.zip;

import net.sf.beezle.sushi.fs.DeleteException;
import net.sf.beezle.sushi.fs.LengthException;
import net.sf.beezle.sushi.fs.ListException;
import net.sf.beezle.sushi.fs.MkdirException;
import net.sf.beezle.sushi.fs.MoveException;
import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.SetLastModifiedException;
import net.sf.beezle.sushi.fs.WriteToException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public Node move(Node dest) throws MoveException {
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
    public InputStream createInputStream() throws IOException {
        ZipFile zip;
        ZipEntry entry;

        zip = root.getZip();
        entry = zip.getEntry(path);
        if (entry == null) {
            throw new FileNotFoundException(path);
        }
        return zip.getInputStream(entry);
    }

    public long writeTo(OutputStream dest, long skip) throws WriteToException, FileNotFoundException {
        return writeToImpl(dest, skip);
    }

    @Override
    public OutputStream createOutputStream(boolean append) {
        throw unsupported("createOutputStream(" + append + ")");
    }

    @Override
    public List<ZipNode> list() throws ListException {
        List<String> paths;
        List<ZipNode> result;

        if (isFile()) {
            return null;
        }
        paths = root.list(path);
        if (paths.size() == 0 && root.getZip().getEntry(path + "/") == null) {
            throw new ListException(this, new FileNotFoundException(path));
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
