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
package net.sf.beezle.sushi.metadata;

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
