/**
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

public class Source {
    public static final int END = -1;
    
    private final String line;
    private int idx;
    private final int max;

    public Source(String line) {
        this.line = line;
        this.idx = 0;
        this.max = line.length();
    }
    
    public int peek() {
        return idx < max ? line.charAt(idx) : END;        
    }
    
    public int peekNext() {
        return idx + 1 < max ? line.charAt(idx + 1) : END;
    }
    
    public void eat() {
        idx++;
    }
    
    public boolean eat(String keyword, char separator) {
        int end;
        
        if (line.indexOf(keyword, idx) == idx) {
            end = idx + keyword.length();
            if (end == max || line.charAt(end) == separator) {
                idx = end;
                return true;
            }
        }
        return false;
    }
}
