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
package net.sf.beezle.sushi.fs.memory;

import net.sf.beezle.sushi.fs.Features;
import net.sf.beezle.sushi.fs.Filesystem;
import net.sf.beezle.sushi.fs.NodeInstantiationException;
import net.sf.beezle.sushi.fs.World;

import java.net.URI;
import java.util.WeakHashMap;

public class MemoryFilesystem extends Filesystem {
    private final WeakHashMap<Integer, MemoryRoot> roots;

    public final int maxInMemorySize;

    public MemoryFilesystem(World world, String name) {
        super(world, new Features(true, false, false, false, false, false, false), name);

        this.roots = new WeakHashMap<Integer, MemoryRoot>();
        this.maxInMemorySize = 32 * 1024;
    }

    @Override
    public MemoryNode node(URI uri, Object extra) throws NodeInstantiationException {
        MemoryRoot result;

        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkHierarchical(uri);
        try {
            result = root(Integer.parseInt(uri.getAuthority()));
        } catch (NumberFormatException e) {
            throw new NodeInstantiationException(uri, "invalid root: " + uri.getAuthority(), e);
        }
        return result.node(getCheckedPath(uri), null);
    }

    public MemoryRoot root(int id) {
        MemoryRoot root;

        root = roots.get(id);
        if (root == null) {
            root = new MemoryRoot(this, id);
            roots.put(id, root);
        }
        return root;
    }

    public MemoryRoot root() {
        MemoryRoot root;

        for (int id = 0; true; id++) {
            if (!roots.containsKey(id)) {
                root = new MemoryRoot(this, id);
                roots.put(id, root);
                return root;
            }
        }
    }
}
