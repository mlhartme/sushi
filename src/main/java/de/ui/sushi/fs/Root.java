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

package de.ui.sushi.fs;

/** 
 * A file system root like the drive letter on Windows or the Host Name for HttpFilesystem. 
 * The root is not a node. A root node is a node with an empty path. 
 */
public interface Root {
    /** Backlink */
    Filesystem getFilesystem();
    
    String getId();
    
    /**
     * Creates a new node with no base. The path has already been checked syntactically. 
     * Never called with heading or tailing separator. The base of the resulting node must be null.
     * TODO: expect splitted path? 
     */
    Node node(String path);
    
    //-- capabilities
    
    boolean canLink();
}
