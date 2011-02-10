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

package net.sf.beezle.sushi.csv;

import net.sf.beezle.sushi.metadata.Instance;
import net.sf.beezle.sushi.metadata.Path;
import net.sf.beezle.sushi.metadata.SimpleTypeException;
import net.sf.beezle.sushi.metadata.Variable;
import net.sf.beezle.sushi.metadata.annotation.Type;
import net.sf.beezle.sushi.metadata.annotation.Value;

import java.util.List;

@Type public class Field {
    @Value private String name;
    @Value private Path path;
    
    public Field() { // TODO
        this("", new Path(""));
    }

    public Field(String name, Path path) {
        this.name = name;
        this.path = path;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public List<String> get(Instance<?> context) {
        Variable<?> var;

        var = path.access(context, false);
        if (var == null) {
            return null;
        } else {
            return var.getStrings();
        }
    }
    
    public void set(Instance<?> context, List<String> values) throws SimpleTypeException {
        if (values == null) {
            // ignore
        } else {
            path.access(context, true).setStrings(values);
        }
    }

    @Override
    public String toString() {
        return name + ":" + path.toString();
    }
}
