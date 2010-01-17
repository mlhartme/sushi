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

package de.ui.sushi.fs.memory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import de.ui.sushi.fs.*;
import de.ui.sushi.io.CheckedByteArrayOutputStream;

/** You'll normally use IO.stringNode() to create instances */
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
    public MemoryRoot getRoot() {
        return root;
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
        try {
            parentDir = getParent().isDirectory();
        } catch (ExistsException e) {
            throw new IllegalStateException(e);
        }
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
            throw new DeleteException(this, new FileNotFoundException());
        }
        if (type == Type.DIRECTORY) {
            for (MemoryNode obj : list()) {
                ((MemoryNode) obj).delete();
            }
        }
        type = Type.NONE;
        return this;
    }

    @Override
    public List<MemoryNode> list() {
        if (type != Type.DIRECTORY) {
            return null;
        }
        try {
            return root.list(path);
        } catch (IOException e) {
            throw new RuntimeException("TODO", e);
        }
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
        ByteArrayOutputStream out;
        
        if (type == Type.DIRECTORY) {
            throw new IOException("cannot write file over directory: " + path);
        }
        getParent().checkDirectory();
        out = new CheckedByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                type = Type.FILE;
                root.store(path, this.buf, this.count);
                super.close();
                lastModified = System.currentTimeMillis();
            }
        };
        if (append && isFile()) {
            out.write(readBytes());
        }
        return out;
    }

    @Override
    public String getPath() {
        return path;
    }
}
