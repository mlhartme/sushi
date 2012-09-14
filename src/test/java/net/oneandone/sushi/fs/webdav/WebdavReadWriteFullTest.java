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
package net.oneandone.sushi.fs.webdav;

import net.oneandone.sushi.TestProperties;
import net.oneandone.sushi.fs.NodeTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class WebdavReadWriteFullTest extends NodeTest<WebdavNode> {
    static {
        WebdavFilesystem.wireLog(WORLD.guessProjectHome(WebdavNodeFullBase.class).getAbsolute() + "/target/webdav-readwrite.log");
    }

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return TestProperties.getParameterList("webdav.readwrite.uri");
    }

    private final String uri;

    public WebdavReadWriteFullTest(String uri) {
        this.uri = uri;
    }

    @Override
    protected WebdavNode createWork() throws IOException {
        return (WebdavNode) WORLD.validNode(uri).deleteTreeOpt().mkdir();
    }

    public void validateDeallocation() {
        assertEquals(0, work.getRoot().getAllocated());
    }

    @Test
    public void attributeFile() throws IOException {
        WebdavNode node;

        node = work.join("file");
        try {
            node.getAttribute("foo");
            fail();
        } catch (WebdavException e) {
            // ok
        }
        node.writeBytes();
        attrib(node, "foo");
    }

    @Test
    public void attributeDir() throws IOException {
        WebdavNode node;

        node = work.join("dir");
        node.mkdir();
        attrib(node, "foo");
    }

    private void attrib(WebdavNode node, String name) throws IOException {
        WebdavNode second;

        assertNull(node.getAttribute(name));
        node.setAttribute(name, "bar");
        assertEquals("bar", node.getAttribute(name));
        node.setAttribute(name, "baz");
        assertEquals("baz", node.getAttribute(name));
        second = node.getParent().join("copy");
        node.copy(second);
        assertEquals("baz", node.getAttribute(name));
        assertNull(second.getAttribute(name));
        second = node.getParent().join("moved");
        node.move(second);
        assertEquals("baz", second.getAttribute(name));
    }
}

