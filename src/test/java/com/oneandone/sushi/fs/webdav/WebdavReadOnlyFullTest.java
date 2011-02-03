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

package de.ui.sushi.fs.webdav;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import de.ui.sushi.TestProperties;
import org.junit.Test;
import de.ui.sushi.fs.Node;
import de.ui.sushi.fs.NodeReadOnlyTest;
import de.ui.sushi.fs.webdav.WebdavNode;
import org.junit.runners.Parameterized;

/** Accesses external hosts and might need proxy configuration => Full test */
public class WebdavReadOnlyFullTest extends NodeReadOnlyTest {
    @Test
    public void normal() throws Exception {
        WebdavNode node;

        node = (WebdavNode) IO.node("http://englishediting.de/index.html");
        assertTrue(node.isFile());
        assertTrue(node.exists());
        assertTrue(node.readString().length() > 1);
        assertEquals("//englishediting.de/", node.getRoot().getId());
        assertEquals("index.html", node.getPath());
        assertEquals("", node.getParent().getPath());
    }

    @Test
    public void node() throws Exception {
        String url;
        WebdavNode node;

        url = "http://englishediting.de/index.html";
        node = (WebdavNode) IO.node(url);
        assertEquals("index.html", node.getPath());
        assertEquals(url, node.getUrl().toString());
    }

    @Override
    protected Node createWork() throws IOException {
        return IO.validNode("http://englishediting.de/foo");
    }
}

