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

package com.oneandone.sushi.csv;

import java.util.ArrayList;
import java.util.List;

import com.oneandone.sushi.util.Misc;

/** A list of cells, where each cell is a list of values */
public class Line {
    public static Line create(String ... values) {
        Line line;
        
        line = new Line();
        for (String value : values) {
            line.add().add(value);
        }
        return line;
    }
    
    private final List<List<String>> cells;
    
    public Line() {
        this.cells = new ArrayList<List<String>>();
    }
    
    public boolean equalsAfter(int ofs, Line line) {
        int max;
        
        max = cells.size();
        if (max != line.cells.size()) {
            return false;
        }
        for (int i = ofs; i < max; i++) {
            if (!Misc.eq(cells.get(i), line.cells.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    public void addOne(String value) {
        add().add(value);
    }
    
    public List<String> add() {
        List<String> cell;
        
        cell = new ArrayList<String>();
        cells.add(cell);
        return cell;
    }
    
    public void addNull() {
        cells.add(null);
    }

    public void merge(Line line, int ofs) {
        cells.get(ofs).addAll(line.cells.get(ofs));
    }
    
    public List<String> get(int idx) {
        return cells.get(idx);
    }

    public String getOne(int idx) {
        List<String> cell;
        
        cell = cells.get(idx);
        switch (cell.size()) {
        case 0:
            throw new ViewException("cell is empty: " + idx);
        case 1:
            return cell.get(0);
        default:
            throw new ViewException("cell with multiple values");
        }
    }

    public int size() {
        return cells.size();
    }
}
