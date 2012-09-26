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
package net.oneandone.sushi.fs.memory;

import net.oneandone.sushi.fs.*;
import net.oneandone.sushi.io.CheckedByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.NoSuchFileException;
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
    public MemoryNode(MemoryRoot root, String path, Type type) {
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
    public MemoryNode deleteFile() throws DeleteException {
        if (type != Type.FILE) {
            throw new DeleteException(this, "file expected");
        }
        type = Type.NONE;
        return this;
    }

    @Override
    public MemoryNode deleteDirectory() throws DeleteException {
        if (type != Type.DIRECTORY) {
            throw new DeleteException(this, "directory expected");
        }
        if (root.list(path).size() > 0) {
            throw new DeleteException(this, "directory is not empty");
        }
        type = Type.NONE;
        return this;
    }

    @Override
    public MemoryNode deleteTree() throws DeleteException {
        if (type == Type.NONE) {
            throw new DeleteException(this, new NoSuchFileException(getPath()));
        }
        if (type == Type.DIRECTORY) {
            try {
                for (MemoryNode obj : list()) {
                    obj.deleteTree();
                }
            } catch (ListException | DirectoryNotFoundException e) {
                throw new DeleteException(this, e);
            }
        }
        type = Type.NONE;
        return this;
    }

    @Override
    public List<MemoryNode> list() throws ListException, DirectoryNotFoundException {
        switch (type) {
            case NONE:
                throw new DirectoryNotFoundException(this);
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
            throw new FileNotFoundException(this);
        }
        return root.readBytes(path);
    }

    @Override
    public InputStream createInputStream() throws FileNotFoundException, CreateInputStreamException {
        if (type != Type.FILE) {
            throw new FileNotFoundException(this);
        }
        try {
            return root.open(path);
        } catch (IOException e) {
            throw new CreateInputStreamException(this, e);
        }
    }

    public long writeTo(OutputStream dest, long skip) throws WriteToException, FileNotFoundException {
        return writeToImpl(dest, skip);
    }

    @Override
    public OutputStream createOutputStream(boolean append) throws FileNotFoundException, CreateOutputStreamException {
        byte[] add;

        try {
            switch (type) {
                case DIRECTORY:
                    throw new FileNotFoundException(this, "cannot write file over directory");
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
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new CreateOutputStreamException(this, e);
        }
    }

    @Override
    public String getPath() {
        return path;
    }
}
