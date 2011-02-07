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

package com.oneandone.sushi;

import com.oneandone.sushi.fs.World;
import com.oneandone.sushi.fs.Node;

public class FsSample {
    /** print all Java files in your src/main/java directory */
    public static void main(String[] args) throws Exception {
        World world;
        Node dir;

        world = new World();
        dir = world.file("src/main/java");
        for (Node node : dir.find("**/*.java")) {
            System.out.println(node.readString());
        }
    }
}
