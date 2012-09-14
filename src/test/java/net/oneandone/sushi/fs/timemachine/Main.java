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
package net.oneandone.sushi.fs.timemachine;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;

public class Main {
    public static void main(String[] args) throws Exception {
        World world;
        Node tm;
        
        world = new World();
        tm = world.node("timemachine:/media/timemachine!harald/Latest/Platte/Users/mhm/Pictures");
        tm.checkDirectory();
        for (Node item : tm.list()) {
            System.out.println(item.getName());
        }
    }
}
