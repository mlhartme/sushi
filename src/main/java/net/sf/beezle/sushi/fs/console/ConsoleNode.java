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

package net.sf.beezle.sushi.fs.console;

import net.sf.beezle.sushi.fs.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ConsoleNode extends Node {
    private final ConsoleFilesystem root;
    
    public ConsoleNode(ConsoleFilesystem root) {
        super();
        
        this.root = root;
    }

    @Override
    public ConsoleFilesystem getRoot() {
        return root;
    }
    
    @Override
    public ConsoleNode getParent() {
        return (ConsoleNode) doGetParent();
    }

    @Override
    public ConsoleNode join(String ... paths) {
        return (ConsoleNode) doJoin(paths);
    }

    @Override
    public ConsoleNode join(List<String> paths) {
        return (ConsoleNode) doJoin(paths);
    }

    @Override
    public List<ConsoleNode> list() {
        return null;
    }

    @Override
    public InputStream createInputStream() throws IOException {
        return System.in;
    }

    /** @parem append is ignored */
    @Override
    public OutputStream createOutputStream(boolean append) throws IOException {
        return System.out;
    }

    @Override
    public Node delete() throws DeleteException {
        throw new DeleteException(this);
    }

    @Override
    public Node move(Node dest) throws MoveException {
    	throw new MoveException(this, dest, "ConsoleNode cannot be moved");
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public boolean isLink() {
    	return false;
    }

    @Override
    public long length() {
        throw unsupported("length()");
    }

    @Override
    public long getLastModified() {
        return System.currentTimeMillis();
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
}
