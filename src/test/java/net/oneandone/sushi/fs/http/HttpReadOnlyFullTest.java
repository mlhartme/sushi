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
package net.oneandone.sushi.fs.http;

import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeReadOnlyTest;
import net.oneandone.sushi.fs.SizeException;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Accesses external hosts and might need proxy configuration =&gt; Full test */
public class HttpReadOnlyFullTest extends NodeReadOnlyTest<HttpNode> {
    static {
        HttpFilesystem.wireLog(WORLD.guessProjectHome(HttpNodeFullBase.class).getAbsolute() + "/target/http-readonly.log");
    }

    public void validateDeallocation() {
        assertEquals(0, work.getRoot().getAllocated());
    }

    @Test
    public void eq() throws Exception {
        HttpNode node;

        node = (HttpNode) WORLD.node("http://englishediting.de/index.html?foo=1");
        assertEquals(node, WORLD.node("http://englishediting.de/index.html?foo=1"));
        assertFalse(node.equals(WORLD.node("http://englishediting.de/index.html?foo=2")));
    }

    private static final String GITHUB = "https://github.com/mlhartme/sushi";

    @Test
    public void englishediting() throws Exception {
        HttpNode node;
        int size;

        node = (HttpNode) WORLD.node("http://englishediting.de/index.html");
        assertTrue(node.isFile());
        assertTrue(node.exists());
        assertTrue(node.getLastModified() != 0);
        size = node.readBytes().length;
        assertEquals(size, node.size());
        assertEquals("//englishediting.de/", node.getRoot().getId());
        assertEquals("index.html", node.getPath());
        assertEquals("", node.getParent().getPath());
    }

    @Test
    public void github() throws Exception {
        assertFalse(WORLD.node(GITHUB + "nosuchfile").exists());
        assertTrue(WORLD.node(GITHUB).exists());
        assertTrue(WORLD.node(GITHUB).isFile());
        try {
            assertTrue(WORLD.node(GITHUB).getLastModified() != 0);
            fail();
        } catch (GetLastModifiedException e) {
            // ok -- github does not return last-modified
        }
        try {
            WORLD.node(GITHUB).size();
            fail();
        } catch (SizeException e) {
            // ok -- github does not return a size
        }

    }

    @Test
    public void uriWithEmptyPath() throws Exception {
        Node node;

        node = WORLD.node("http://www.heise.de");
        assertTrue(node instanceof HttpNode);
        assertEquals("http://www.heise.de:80/", node.getURI().toString());
        assertNotNull(node.readBytes());
    }

    @Test
    public void query() throws Exception {
        URI uri;
        HttpNode node;
        String str;

        uri = URI.create("http://dict.tu-chemnitz.de:80/dings.cgi?lang=en&noframes=1&service=&query=foobarbaz&optword=1&optcase=1&opterrors=0&optpro=0&style=&dlink=self");
        node = (HttpNode) WORLD.node(uri);
        assertEquals(uri, node.getURI());
        assertTrue(node.isFile());
        str = node.readString();
        assertTrue(str.contains("foobarbaz"));
    }

    @Test
    public void node() throws Exception {
        URI uri;
        HttpNode node;

        uri = new URI("http://englishediting.de:80/index.html");
        node = (HttpNode) WORLD.node(uri);
        assertEquals("index.html", node.getPath());
        assertEquals(uri, node.getURI());
    }

    @Override
    protected HttpNode createWork() throws IOException {
        return (HttpNode) WORLD.validNode("http://englishediting.de/foo");
    }
}

