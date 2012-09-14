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
package net.oneandone.sushi.metadata;

public class Cardinality {
    public static final Cardinality OPTION = new Cardinality(0, 1);
    public static final Cardinality VALUE = new Cardinality(1, 1);
    public static final Cardinality SEQUENCE = new Cardinality(0, Integer.MAX_VALUE);
    
    public final int min;
    public final int max;

    private Cardinality(int min, int max) {
        this.min = min;
        this.max = max;
    }
    
    public boolean isOptional() {
        return min == 0;
    }
    
    public boolean isUnbounded() {
        return max == Integer.MAX_VALUE;
    }
    
    public String forSchema() {
        StringBuilder builder;
        
        builder = new StringBuilder();
        if (min != 1) {
            builder.append(" minOccurs='");
            builder.append(min);
            builder.append("'");
        }
        if (isUnbounded()) {
            builder.append(" maxOccurs='unbounded'");
        } else if (max != 1) {
            builder.append(" '");
            builder.append(max);
            builder.append("'");
        }
        return builder.toString();
    }
}
