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

import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.LengthException;
import net.oneandone.sushi.fs.Root;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.CheckedByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryRoot implements Root<MemoryNode> {
    public final MemoryFilesystem filesystem;
    public final int id;
    private final Map<String, MemoryNode> nodes;
    private final Map<String, Object> store;
    
    public MemoryRoot(MemoryFilesystem filesystem, int id) {
        this.filesystem = filesystem;
        this.id = id;
        this.nodes = new HashMap<String, MemoryNode>();
        this.store = new HashMap<String, Object>();
        add(new MemoryNode(this, "", Type.DIRECTORY));
    }

    public MemoryFilesystem getFilesystem() {
        return filesystem;
    }

    public String getId() {
        return "//" + id + "/";
    }

    public MemoryNode node(String path, String encodedQuery) {
        MemoryNode node;
        
        if (encodedQuery != null) {
            throw new IllegalArgumentException(encodedQuery);
        }
        node = nodes.get(path);
        if (node == null) {
            node = new MemoryNode(this, path, Type.NONE);
            nodes.put(node.getPath(), node);
        }
        return node;
    }
    
    public void add(MemoryNode node) {
        nodes.put(node.getPath(), node);
    }
    
    public long length(String path) throws LengthException {
        Object obj;

        obj = store.get(path);
        if (obj instanceof FileNode) {
            return ((FileNode) obj).length();
        } else {
            return ((byte[]) obj).length;
        }
    }
    
    public List<MemoryNode> list(String path) {
        String child;
        int idx;
        List<MemoryNode> result;
        
        result = new ArrayList<MemoryNode>();
        for (MemoryNode node : nodes.values()) {
            child = node.getPath();
            idx = child.lastIndexOf(Filesystem.SEPARATOR_CHAR);
            if (!path.equals(child) && path.equals(idx == -1 ? "" : child.substring(0, idx))) {
                if (node.exists()) {
                    result.add(node);
                }
            }
        }     
        return result;
    }
    
    //--
    
    InputStream open(String path) throws IOException {
        Object obj;
        
        obj = store.get(path);
        if (obj instanceof FileNode) {
            return ((FileNode) obj).createInputStream();
        } else {
            return new CheckedByteArrayInputStream((byte[]) obj);
        }
    }

    byte[] readBytes(String path) throws IOException {
        Object obj;
        byte[] bytes;

        obj = store.get(path);
        if (obj instanceof FileNode) {
            return ((FileNode) obj).readBytes();
        } else {
            bytes = (byte[]) obj;
            return Arrays.copyOf(bytes, bytes.length);
        }
    }
    
    void store(String path, byte[] data, int used) throws IOException {
        Object old;
        FileNode file;
        byte[] copy;
        
        old = store.get(path);
        if (old instanceof FileNode) {
            ((FileNode) old).deleteTree();
        }
        if (used > filesystem.getMaxInMemorySize()) {
            file = filesystem.getWorld().getTemp().createTempFile();
            file.writeBytes(data, 0, used, false);
            store.put(path, file);
        } else {
            copy = new byte[used];
            System.arraycopy(data, 0, copy, 0, used);
            store.put(path, copy);
        }
    }
}
