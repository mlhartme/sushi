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

import net.sf.beezle.sushi.fs.LengthException;
import net.sf.beezle.sushi.fs.Root;
import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.io.CheckedByteArrayInputStream;

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
        add(new MemoryNode(this, "", Type.DIRECTORY, null));
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
            node = new MemoryNode(this, path, Type.NONE, null);
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
            idx = child.lastIndexOf(node.getRoot().getFilesystem().getSeparatorChar());
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
            ((FileNode) old).delete();
        }
        if (used > filesystem.maxInMemorySize) {
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
