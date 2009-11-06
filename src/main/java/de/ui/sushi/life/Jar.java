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

package de.ui.sushi.life;

import java.util.ArrayList;
import java.util.List;

import de.ui.sushi.metadata.annotation.Sequence;
import de.ui.sushi.metadata.annotation.Type;
import de.ui.sushi.metadata.annotation.Value;

@Type
public class Jar {
    @Value
    private Id id;

    @Sequence(String.class)
    private List<String> directories;
    
    public Jar() {
        this(new Id());
    }
    
    public Jar(String str) {
        this(Id.fromString(str));
    }
    
    public Jar(Id id) {
        this.id = id;
        this.directories = new ArrayList<String>();
    }
    
    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }
    
    public List<String> directories() {
        return directories;
    }
}
