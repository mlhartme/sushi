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

package net.sf.beezle.sushi.fs.timemachine;

import net.sf.beezle.sushi.fs.DeleteException;
import net.sf.beezle.sushi.fs.ExistsException;
import net.sf.beezle.sushi.fs.Filesystem;
import net.sf.beezle.sushi.fs.GetLastModifiedException;
import net.sf.beezle.sushi.fs.LengthException;
import net.sf.beezle.sushi.fs.LinkException;
import net.sf.beezle.sushi.fs.ListException;
import net.sf.beezle.sushi.fs.MkdirException;
import net.sf.beezle.sushi.fs.MoveException;
import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.ReadLinkException;
import net.sf.beezle.sushi.fs.SetLastModifiedException;
import net.sf.beezle.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class TimeMachineNode extends Node {
    private final TimeMachineRoot root;
    private final FileNode node;
    private final String path;
    
    // CAUTION: url is not checked for url parameter
    public TimeMachineNode(TimeMachineRoot root, FileNode node, String path) {
        this.root = root;
        this.node = node;
        this.path = path;
    }

    @Override
    public TimeMachineRoot getRoot() {
        return root;
    }

    @Override
    public TimeMachineNode getParent() {
        return (TimeMachineNode) doGetParent();
    }

    @Override
    public TimeMachineNode join(String ... paths) {
        return (TimeMachineNode) doJoin(paths);
    }

    @Override
    public TimeMachineNode join(List<String> paths) {
        return (TimeMachineNode) doJoin(paths);
    }

    @Override
    public long length() throws LengthException {
        return node.length();
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        return node.getLastModified();
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
    public String getPath() {
        return path;
    }


    @Override
    public Node delete() throws DeleteException {
        throw unsupported("delete()");
    }

    @Override
    public Node move(Node dest) throws MoveException {
        throw unsupported("move()");
    }

    @Override
    public TimeMachineNode mkdir() throws MkdirException {
        throw unsupported("delete");
    }

    @Override
    public void mklink(String target) throws LinkException {
        node.mklink(target);
    }

    @Override
    public String readLink() throws ReadLinkException {
        return node.readLink();
    }

    @Override
    public boolean exists() throws ExistsException {
        return node.exists();
    }

    @Override
    public boolean isFile() throws ExistsException {
        return node.isFile();
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        return node.isDirectory();
    }

    @Override
    public boolean isLink() throws ExistsException {
    	return node.isLink();
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return node.createInputStream();
    }

    public long writeTo(OutputStream dest, long skip) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream createOutputStream(boolean append) throws IOException {
        throw unsupported("createOutputStream(boolean)");
    }

    @Override
    public List<TimeMachineNode> list() throws ListException {
        List<FileNode> files;
        List<TimeMachineNode> result;
        Filesystem fs;
        
        files = node.list();
        if (files == null) {
            return null;
        }
        result = new ArrayList<TimeMachineNode>(files.size());
        fs = root.getFilesystem();
        for (FileNode file : files) {
            try {
                result.add(new TimeMachineNode(root, root.resolve(file), fs.join(path, file.getName())));
            } catch (IOException e) {
                throw new ListException(this, e);
            }
        }
        return result;
    }
}
