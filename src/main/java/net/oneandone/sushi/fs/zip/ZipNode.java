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
package net.oneandone.sushi.fs.zip;

import net.oneandone.sushi.fs.CopyFileFromException;
import net.oneandone.sushi.fs.CopyFileToException;
import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.MoveException;
import net.oneandone.sushi.fs.NewInputStreamException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.SetLastModifiedException;
import net.oneandone.sushi.fs.SizeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Represents an entry in a zip or jar file. Read-only
 */
public class ZipNode extends Node<ZipNode> {
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
    public long size() throws SizeException {
        ZipEntry entry;

        entry = root.getZip().getEntry(path);
        if (entry == null) {
            throw new SizeException(this, new IOException("file expected"));
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
    public ZipNode deleteFile() throws DeleteException {
        throw new DeleteException(this);
    }

    @Override
    public ZipNode deleteDirectory() throws DeleteException {
        throw new DeleteException(this);
    }

    @Override
    public ZipNode deleteTree() throws DeleteException {
        throw new DeleteException(this);
    }

    @Override
    public ZipNode move(Node dest, boolean overwrite) throws MoveException {
        throw new MoveException(this, dest, "ZipNode cannot be moved");
    }

    @Override
    public ZipNode mkdir() throws MkdirException {
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
        return root.getZip().getEntry(path) != null || isDirectory();
    }

    @Override
    public boolean isFile() throws ExistsException {
        ZipEntry entry;
        InputStream in;

        entry = root.getZip().getEntry(path);
        if (entry == null) {
            // not found or implicit directory
            return false;
        }
        if (entry.isDirectory()) {
            return false;
        }

        // Note: the rest of this method is a work-around for https://bugs.openjdk.java.net/browse/JDK-6233323
        // (isDirectory does not properly report a directory if the initial path is without tailing /):
        // try to read this as a file

        if (entry.getSize() > 0) {
            return true;
        }
        try {
            // differs for directories on different jdks:
            // pre-1.8.0_144: returns null
            // 1.8.0_144 and later: returns empty file
            in = root.getZip().getInputStream(entry);
        } catch (IOException e) {
            throw new ExistsException(this, e);
        }
        if (in == null) {
            return false;
        } else {
            try {
                in.close();
            } catch (IOException e) {
                throw new ExistsException(this, e);
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
            // root directory
            return true;
        }
        zip = root.getZip();
        if (zip.getEntry(path + "/") != null) {
            return true;
        }
        // this might be an implicit directory - try to list it
        return root.list(path).size() > 0;
    }

    @Override
    public InputStream newInputStream() throws FileNotFoundException, NewInputStreamException {
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
            throw new NewInputStreamException(this, e);
        }
    }

    public long copyFileTo(OutputStream dest, long skip) throws CopyFileToException, FileNotFoundException {
        return copyFileToImpl(dest, skip);
    }

    public void copyFileFrom(InputStream dest) throws FileNotFoundException, CopyFileFromException {
        copyFileFromImpl(dest);
    }

    @Override
    public OutputStream newOutputStream(boolean append) {
        throw unsupported("newOutputStream(" + append + ")");
    }

    @Override
    public List<ZipNode> list() throws DirectoryNotFoundException, ListException {
        List<String> paths;
        List<ZipNode> result;

        try {
            if (isFile()) {
                return null;
            }
        } catch (ExistsException e) {
            throw new ListException(this, e);
        }
        paths = root.list(path);
        if (paths.size() == 0 && root.getZip().getEntry(path + "/") == null) {
            throw new DirectoryNotFoundException(this);
        }
        result = new ArrayList<>();
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
