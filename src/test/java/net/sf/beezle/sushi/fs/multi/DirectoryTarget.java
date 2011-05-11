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

package net.sf.beezle.sushi.fs.multi;

import net.sf.beezle.sushi.fs.Node;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

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
