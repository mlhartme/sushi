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
package net.oneandone.sushi.fs.multi;

import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirectoryTarget {
    public static DirectoryTarget create(Node dir, int children) throws IOException {
        dir.mkdir();

        for (int i = 1; i <= children; i++) {
            dir.join("d" + i).writeBytes();
        }
        return new DirectoryTarget(dir);
    }

    //--

    private final Node dir;
    private final List<?> children;

    public DirectoryTarget(Node dir) throws IOException {
        this.dir = dir;
        this.children = dir.list();
    }

    public void isDirectory() throws IOException {
        assertTrue(dir.isDirectory());
    }

    public void isFile() throws IOException {
        assertFalse(dir.isFile());
    }

    public void exists() throws IOException {
        assertTrue(dir.exists());
    }

    public void list() throws IOException {
        assertEquals(children, dir.list());
    }

    public void find() throws IOException {
        List<? extends Node> lst;

        lst = dir.find("*");
        for (Node node : lst) {
            assertTrue(children.contains(node));
        }
        assertEquals(children.size(), lst.size());
    }
}
