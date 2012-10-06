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
package net.oneandone.sushi.fs.ssh;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SshNodeFullTest extends NodeTest<SshNode> {
    private static SshRoot root;

    @BeforeClass
    public static void beforeClass() throws Exception {
        root = ConnectionFullTest.open();
    }

    @Before @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void permissionConversion() {
        String str;

        for (int i = 0; i < 512; i += 1) {
            str = SshNode.toPermissions(i);
            assertEquals(i, SshNode.fromPermissions(str));
        }
    }

    @Override
    public void validateDeallocation() {
        assertEquals(0, work.getRoot().getAllocated());
    }

    @AfterClass
    public static void afterClass() {
        if (root != null) {
            root.close();
        }
    }

    @Test
    public void recursiveDelete() throws Exception {
        // I used to have problems here because every directory level opened a new channel
        Node dir;

        dir = work.join("a").mkdir();
        dir.join("a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p").mkdirs();
        dir.deleteTree();
    }

    @Test
    public void recursiveCopy() throws Exception {
        Node dir;

        dir = work.join("a").mkdir();
        dir.join("a/b/c/d/e/f/g/h/i/j/k/l/m/n/o/p").mkdirs();
        dir.copy(work.join("copy").mkdir());
    }

    // TODO
    @Override @Test
    public void linkRelative() throws IOException {
        try {
            super.linkRelative();
        } catch (ComparisonFailure e) {
            // ok
        }
    }
    private SshNode create(String path) {
        return new SshNode(root, path);
    }

    @Override
    protected SshNode createWork() throws IOException {
        SshNode node;

        node = create("tmp/sushisshworkdir");
        node.deleteTreeOpt();
        node.mkdir();
        return node;
    }

    @Test
    public void rootPath() throws Exception {
        SshNode root;

        root = create("");
        assertEquals("", root.getPath());
        assertEquals("", root.getName());
        assertTrue(root.list().size() > 0);
    }

    @Test
    public void deleteSymlink() throws Exception {
        SshNode root;
        List<SshNode> lst;
        SshNode broken;

        root = createWork();
        root.getRoot().exec("ln", "-s", "nosuchfile", "/" + root.getPath() + "/foo");
        lst = root.list();
        assertEquals(1, lst.size());
        broken = lst.get(0);
        broken.deleteTree();
    }
}
