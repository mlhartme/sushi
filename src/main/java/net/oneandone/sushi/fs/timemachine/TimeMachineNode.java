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
package net.oneandone.sushi.fs.timemachine;

import net.oneandone.sushi.fs.*;
import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
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
    public String getPath() {
        return path;
    }


    @Override
    public Node deleteFile() throws DeleteException {
        throw unsupported("deleteFile()");
    }

    @Override
    public Node deleteDirectory() throws DeleteException {
        throw unsupported("deleteDirectory()");
    }

    @Override
    public Node deleteTree() throws DeleteException {
        throw unsupported("deleteTree()");
    }

    @Override
    public Node move(Node dest, boolean overwrite) throws MoveException {
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
    public InputStream createInputStream() throws FileNotFoundException, CreateInputStreamException {
        return node.createInputStream();
    }

    public long writeTo(OutputStream dest, long skip) throws FileNotFoundException, WriteToException {
        return writeToImpl(dest, skip);
    }

    @Override
    public OutputStream createOutputStream(boolean append) {
        throw unsupported("createOutputStream(boolean)");
    }

    @Override
    public List<TimeMachineNode> list() throws ListException, DirectoryNotFoundException {
        List<FileNode> files;
        List<TimeMachineNode> result;
        Filesystem fs;

        files = node.list();
        if (files == null) {
            return null;
        }
        result = new ArrayList<>(files.size());
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
