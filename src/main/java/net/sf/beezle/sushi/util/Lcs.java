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
