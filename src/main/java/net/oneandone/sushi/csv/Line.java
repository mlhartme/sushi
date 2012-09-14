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
package net.oneandone.sushi.csv;

import net.oneandone.sushi.util.Util;

import java.util.ArrayList;
import java.util.List;

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
            if (!Util.eq(cells.get(i), line.cells.get(i))) {
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
