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

package net.sf.beezle.sushi.fs.zip;

import net.sf.beezle.sushi.fs.DeleteException;
import net.sf.beezle.sushi.fs.MkdirException;
import net.sf.beezle.sushi.fs.MoveException;
import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.SetLastModifiedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
    public long length() {
        return root.getZip().getEntry(path).getSize();
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
    public Node delete() throws DeleteException {
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
        if (entry == null || entry.isDirectory()) {
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
        ZipEntry entry;
        Enumeration<? extends ZipEntry> e;
        String name;
        String separator;
        String prefix;

        if (path.isEmpty()) {
            return true;
        }
        zip = root.getZip();
        e = zip.entries();
        separator = root.getFilesystem().getSeparator();
        prefix = getPath() + separator;
        // TODO: expensive
        while (e.hasMoreElements()) {
            entry = e.nextElement();
            name = entry.getName();
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        ZipFile zip;
        ZipEntry entry;
        
        zip = root.getZip();
        entry = zip.getEntry(path);
        if (entry == null || entry.isDirectory()) {
            throw new FileNotFoundException(path);
        }
        return zip.getInputStream(entry);
    }

    @Override
    public OutputStream createOutputStream(boolean append) {
        throw unsupported("createOutputStream(" + append + ")");
    }

    @Override
    public List<ZipNode> list() {
        ZipEntry entry;
        Enumeration<? extends ZipEntry> e;
        String name;
        String separator;
        String prefix;
        int length;
        List<ZipNode> result;
        List<String> done;
        int idx;
        
        // TODO: expensive
        e = root.getZip().entries();
        separator = root.getFilesystem().getSeparator();
        prefix = path.length() == 0 ? "" : path + separator;
        length = prefix.length();
        result = new ArrayList<ZipNode>();
        done = new ArrayList<String>();
        done.add(path);
        while (e.hasMoreElements()) {
            entry = e.nextElement();
            name = entry.getName();
            if (name.length() > length && name.startsWith(prefix)) {
                idx = name.indexOf(separator, length);
                name = (idx == -1 ? name : name.substring(0, idx));
                if (!done.contains(name)) {
                    done.add(name);
                    result.add(root.node(name, null));
                }
            }
        }
        return result;
    }

    @Override
    public String getPath() {
        return path;
    }
}
