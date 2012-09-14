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
package net.oneandone.sushi.fs.filter;

import net.oneandone.sushi.fs.Node;

import java.io.IOException;
import java.util.Collection;

public class CollectAction implements Action {
    private final Collection<Node> collection;
    
    public CollectAction(Collection<Node> collection) {
        this.collection = collection;
    }
    
    public void enter(Node node, boolean isLink) {
    }

    public void enterFailed(Node node, boolean isLink, IOException e) throws IOException {
        throw e;
    }

    public void leave(Node node, boolean isLink) {
    }
    
    public void select(Node node, boolean isLink) {
        collection.add(node);
    }
}
