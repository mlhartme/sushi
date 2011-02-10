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

package net.sf.beezle.sushi.metadata.xml;

import java.io.IOException;

public abstract class Tree {
    public abstract Object done() throws IOException;
    
    public abstract void ref(String name, int idref) throws IOException;
    public abstract void begin(String name, int id, String typeAttribute, boolean withEnd) throws IOException;
    public abstract void end(String name) throws IOException;
    public abstract void text(String name, String typeAttribute, String text) throws IOException;
}
