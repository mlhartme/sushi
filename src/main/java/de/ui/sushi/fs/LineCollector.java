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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 
 * Mainly used to Node.readLines and testing; applications should use LineProcessor instead, 
 * because it's more efficient.
 */
public class LineCollector extends LineProcessor {
    private final List<String> result;
    
    public LineCollector(int size, boolean trim, boolean empty, String comment) {
        super(size, trim, empty, comment);
        
        result = new ArrayList<String>();
    }

    public List<String> collect(Node node) throws IOException {
        run(node);
        return result;
    }

    @Override
    public void line(String line) {
        result.add(line);
    }
}
