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
package net.oneandone.sushi.fs;

import net.oneandone.sushi.io.Buffer;
import net.oneandone.sushi.io.OS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;

public abstract class NodeReadOnlyTest<T extends Node> {
    protected static final World WORLD = new World(OS.CURRENT, new Settings(), new Buffer(), "**/.svn/**/*").addStandardFilesystems(true);

    /** creates a new empty directory */
    protected abstract T createWork() throws IOException;

    protected T work;

    @Before
    public void setUp() throws Exception {
        work = createWork();
        validateDeallocation();
    }

    @After
    public abstract void validateDeallocation() throws Exception;

    @Test
    public void uri() throws Exception {
        URI uri;
        Node again;
        Filesystem fs;

        fs = work.getRoot().getFilesystem();
        uri = work.getURI();
        assertEquals(uri, work.getWorld().node(fs.getScheme() + ":"
                + work.getRoot().getId() + Node.encodePath(work.getPath())).getURI());
        again = WORLD.node(uri);
        assertEquals(work, again);
        assertEquals(uri, again.getURI());
    }
}
