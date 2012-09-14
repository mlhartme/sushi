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
