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
package net.sf.beezle.sushi.graph;

import java.util.ArrayList;
import java.util.List;

public class Node<T> {
    private static int counter = 0;
    
    private final int hashCode;
    public final T data;
    public final List<Node<T>> starting;
    public final List<Node<T>> ending;
    
    public Node(T data) {
        this.hashCode = counter++;
        this.data = data;
        this.starting = new ArrayList<Node<T>>();
        this.ending = new ArrayList<Node<T>>();
    }
    
    /** Override default implementation to get reproducable results */
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }
}
