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
package net.sf.beezle.sushi.util;

import java.util.ArrayList;
import java.util.List;

public class Lcs<T> {
    public static <T> List<T> compute(List<T> vert, List<T> hor) {
        List<T>[] previous;
        List<T>[] current;
        
        previous = new List[hor.size() + 1];
        for (int i = 0; i < previous.length; i++) {
            previous[i] = new ArrayList<T>();
        }

        for (T value : vert) {
            current = new List[hor.size() + 1];
            current[0] = new ArrayList<T>();
            for (int i = 1; i < current.length; i++) {
                if (value.equals(hor.get(i - 1))) {
                    current[i] = append(previous[i - 1], value);
                } else {
                    current[i] = longest(current[i - 1], previous[i]);
                }
            }
            previous = current;
        }
        return previous[previous.length - 1];
    }
    
    private static <T> List<T> append(List<T> lst, T value) {
        List<T> result;
        
        result = new ArrayList<T>(lst.size() + 1);
        result.addAll(lst);
        result.add(value);
        return result;
    }
    
    private static <T> List<T> longest(List<T> a, List<T> b) {
        return new ArrayList<T>(a.size() >= b.size() ? a : b);
    }
}
