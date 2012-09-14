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
package net.oneandone.sushi.fs;

/** 
 * A file system root like the drive letter on Windows or the Host Name for HttpFilesystem. 
 * The root is not a node. A root node is a node with an empty path. 
 */
public interface Root<T extends Node> {
    /** Backlink */
    Filesystem getFilesystem();

    /** Part of the URI between scheme (including ':') and path */
    String getId();
    
    /**
     * Creates a new node with no base. The path has already been checked syntactically. 
     * Never called with heading or tailing separator. The base of the resulting node must be null.
     * TODO: expect splitted path? 
     */
    T node(String path, String encodedQuery);
}
