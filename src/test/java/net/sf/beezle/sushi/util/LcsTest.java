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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** 
 * Computes the longest common subsequence as described in
 * http://en.wikipedia.org/wiki/Longest_common_subsequence_problem 
 */
public class LcsTest {
    @Test
    public void empty() {
        check("", "", "");
    }
    
    @Test
    public void same() {
        check("a", "a", "a");
        check("abc", "abc", "abc");
    }

    @Test
    public void different() {
        check("", "a", "b");
        check("", "abc", "123");
    }
    
    @Test
    public void mixed() {
        check("ga", "agcat", "gac");
        check("MJAU", "MZJAWXU", "XMJYAUZ");
    }
    
    private void check(String expected, String left, String right) {
        assertEquals(lst(expected), Lcs.compute(lst(left), lst(right)));
    }
    
    private List<Character> lst(String arg) {
        int max;
        List<Character> result;

        max = arg.length();
        result = new ArrayList<Character>(max);
        for (int i = 0; i < max; i++) { 
            result.add(arg.charAt(i));
        }
        return result;
    }
}
