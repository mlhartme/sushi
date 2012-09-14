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
package net.oneandone.sushi;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

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
