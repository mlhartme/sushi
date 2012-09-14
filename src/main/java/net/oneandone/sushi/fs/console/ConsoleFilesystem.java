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
package net.oneandone.sushi.fs.console;

import net.oneandone.sushi.fs.Features;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.NodeInstantiationException;
import net.oneandone.sushi.fs.Root;
import net.oneandone.sushi.fs.World;

import java.net.URI;

public class ConsoleFilesystem extends Filesystem implements Root<ConsoleNode> {
    public ConsoleFilesystem(World world, String name) {
        super(world, new Features(true, false, false, false, false, false, false), name);
    }

    public Filesystem getFilesystem() {
        return this;
    }

    public String getId() {
        return "/";
    }

    // TODO
    @Override
    public ConsoleNode node(String path, String encodedQuery) {
        return new ConsoleNode(this);
    }

    @Override
    public ConsoleNode node(URI uri, Object extra) throws NodeInstantiationException {
        if (extra != null) {
            throw new NodeInstantiationException(uri, "unexpected extra argument: " + extra);
        }
        checkHierarchical(uri);
        if (!SEPARATOR_STRING.equals(uri.getPath())) {
            throw new NodeInstantiationException(uri, "unexpected path");
        }
        return new ConsoleNode(this);
    }
}
