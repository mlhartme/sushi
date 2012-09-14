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
package net.oneandone.sushi.util;

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
