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

package net.sf.beezle.sushi.fs.memory;

import net.sf.beezle.sushi.fs.DeleteException;
import net.sf.beezle.sushi.fs.GetLastModifiedException;
import net.sf.beezle.sushi.fs.LengthException;
import net.sf.beezle.sushi.fs.ListException;
import net.sf.beezle.sushi.fs.MkdirException;
import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.SetLastModifiedException;
import net.sf.beezle.sushi.io.CheckedByteArrayOutputStream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/** You'll normally use World.memoryNode() to create instances */
public class MemoryNode extends Node {
    /** never null */
    private final MemoryRoot root;

    /** never null */
    private final String path;

    /** never null */
    private Type type;

    private long lastModified;

    /** Do not call - use create instead. */
    public MemoryNode(MemoryRoot root, String path, Type type, byte[] data) {
        this.root = root;
        this.path = path;
        this.type = type;
        this.lastModified = 0;
    }

    @Override
    public URI getURI() {
        try {
            return new URI(root.getFilesystem().getScheme(), null, Integer.toString(root.id), -1, "/" + path, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public MemoryRoot getRoot() {
        return root;
    }

    @Override
    public MemoryNode getParent() {
        return (MemoryNode) doGetParent();
    }

    @Override
    public MemoryNode join(String ... paths) {
        return (MemoryNode) doJoin(paths);
    }

    @Override
    public MemoryNode join(List<String> paths) {
        return (MemoryNode) doJoin(paths);
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean exists() {
        return type != Type.NONE;
    }

    @Override
    public boolean isFile() {
        return type == Type.FILE;
    }

    @Override
    public boolean isDirectory() {
        return type == Type.DIRECTORY;
    }

    @Override
    public boolean isLink() {
    	return false;
    }

    @Override
    public long length() throws LengthException {
        if (!isFile()) {
            throw new LengthException(this, new IOException("file expected"));
        }
        return root.length(path);
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        if (type == Type.NONE) {
            throw new GetLastModifiedException(this, null);
        }
        return lastModified;
    }

    @Override
    public void setLastModified(long millis) throws SetLastModifiedException {
        lastModified = millis;
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
    public Node mkdir() throws MkdirException {
        boolean parentDir;

        if (type != Type.NONE) {
            throw new MkdirException(this);
        }
        parentDir = getParent().isDirectory();
        if (!parentDir) {
            throw new MkdirException(this);
        }
        type = Type.DIRECTORY;
        lastModified = System.currentTimeMillis();
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
    public MemoryNode delete() throws DeleteException {
        if (type == Type.NONE) {
            throw new DeleteException(this, new FileNotFoundException(getPath()));
        }
        if (type == Type.DIRECTORY) {
            try {
                for (MemoryNode obj : list()) {
                    obj.delete();
                }
            } catch (ListException e) {
                throw new DeleteException(this, e);
            }
        }
        type = Type.NONE;
        return this;
    }

    @Override
    public List<MemoryNode> list() throws ListException {
        switch (type) {
            case NONE:
                throw new ListException(this, new FileNotFoundException(getPath()));
            case FILE:
                return null;
            case DIRECTORY:
                return root.list(path);
            default:
                throw new IllegalStateException();
        }
    }

    public byte[] readBytes() throws IOException {
        if (type != Type.FILE) {
            throw new FileNotFoundException(path);
        }
        return root.readBytes(path);
    }

    @Override
    public InputStream createInputStream() throws IOException {
        if (type != Type.FILE) {
            throw new FileNotFoundException(path);
        }
        return root.open(path);
    }

    @Override
    public OutputStream createOutputStream(boolean append) throws IOException {
        byte[] add;

        switch (type) {
            case DIRECTORY:
                throw new IOException("cannot write file over directory: " + path);
            case FILE:
                add = append ? readBytes() : null;
                break;
            default:
                add = null;
        }
        getParent().checkDirectory();
        return new CheckedByteArrayOutputStream(add) {
            @Override
            public void close() throws IOException {
                type = Type.FILE;
                root.store(path, this.buf, this.count);
                super.close();
                lastModified = System.currentTimeMillis();
            }
        };
    }

    @Override
    public String getPath() {
        return path;
    }
}
