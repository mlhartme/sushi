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
package net.oneandone.sushi.fs.console;

import net.oneandone.sushi.fs.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
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
    public InputStream createInputStream() {
        return System.in;
    }

    public long writeTo(OutputStream dest, long skip) throws WriteToException, FileNotFoundException {
        throw new UnsupportedOperationException();
    }

    /** @param append is ignored */
    @Override
    public OutputStream createOutputStream(boolean append) {
        return System.out;
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
