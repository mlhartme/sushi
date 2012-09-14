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

/**
 * <p> Set of pairs of ints. </p>
 *
 * <p> TODO: cleanup method naming, dynamically grow the relation
 * if necessary. I wait with cleanup until there is more code that
 * uses relations. </p>
 */

public class IntBitRelation {
    /** storage. */
    private final IntBitSet[] line;

    /**
     * Create a new empty releation.
     * @param size upper bound for int values stored in the relation
     */
    public IntBitRelation(int size) {
        line = new IntBitSet[size]; // initialized to null
    }

    /**
     * Returns the upper bound for int elements in the relation
     * @return  largest int + 1 allowed in this relation
     */
    public int getMax() {
        return line.length;
    }

    /**
     * Adds a new pair to the relation.
     * @param  left   left value of the pair
     * @param  right  right value of the pair
     * @return false, if the pair was already element of the relation
     */
    public boolean add(int left, int right) {
        if (line[left] == null) {
            line[left] = new IntBitSet();
        } else {
            if (line[left].contains(right)) {
                return false;
            }
        }

        line[left].add(right);
        return true;
    }

    /**
     * Adds a set of pairs.
     * @param  left   left value of all pairs to add
     * @param  right  right values of the pairs to add. If empty, no
     *                pair is added
     */
    public void add(int left, IntBitSet right) {
        if (line[left] == null) {
            line[left] = new IntBitSet();
        }
        line[left].addAll(right);
    }

    /**
     * Adds all element from the relation specified.
     * @param  right   relation to add.
     */
    public void addAll(IntBitRelation right) {
        int i, max;

        max = right.line.length;
        for (i = 0; i < max; i++) {
            if (right.line[i] != null) {
                add(i, right.line[i]);
            }
        }
    }

    /**
     * Element test.
     * @param  left  left value of the pair to test.
     * @param  right right value of the pair to test.
     * @return  true, if (left, right) is element of the relation.
     */
    public boolean contains(int left, int right) {
        if (line[left] == null) {
            return false;
        }
        return line[left].contains(right);
    }

    /**
     * Returns all right values from the relation that have the
     * left value specified.
     * @param  left    left value required
     * @param  result  where to return the result
     */
    public void addRightWhere(int left, IntBitSet result) {
        if (line[left] != null) {
            result.addAll(line[left]);
        }
    }


    /**
     * If (x,a) is element of left and (x,b) is element of right,
     * (b,a) is added to this relation.
     * @param left  left relation
     * @param right right relation
     */
    public void composeLeftLeftInv(IntBitRelation left, IntBitRelation right) {
        int i, a, b;
        IntBitSet li, ri;

        for (i = 0; i < line.length; i++) {
            li = left.line[i];
            ri = right.line[i];
            if (li != null && ri != null) {
                for (a = li.first(); a != -1; a = li.next(a)) {
                    for (b = ri.first(); b != -1; b = ri.next(b)) {
                        add(b, a);
                    }
                }
            }
        }
    }

    /**
     * If (a,b) is element of left and (b,c) is element of right,
     * then (a,c) is added to this relation.
     * @param  left  left relation
     * @param  right right relation.
     */
    public void composeRightLeft(IntBitRelation left, IntBitRelation right) {
        int i, ele;
        IntBitSet li;

        for (i = 0; i < left.line.length; i++) {
            li = left.line[i];
            if (li != null) {
                for (ele = li.first(); ele != -1; ele = li.next(ele)) {
                    if (right.line[ele] != null) {
                        add(i, right.line[ele]);
                    }
                }
            }
        }
    }

    /**
     * Comparison.
     * @param  obj  object to compare with
     * @return true, if obj is a relation with exaclty the
     *         the same elements as this relation
     */
    @Override
    public boolean equals(Object obj) {
        IntBitRelation rel;
        int i;

        if (!(obj instanceof IntBitRelation)) {
            return false;
        }
        rel = (IntBitRelation) obj;
        if (line.length != rel.line.length) {
            return false;
        }
        for (i = 0; i < line.length; i++) {
            if ((line[i] != null) && (rel.line[i] != null)) {
                if (!line[i].equals(rel.line[i])) {
                    return false;
                }
            } else {
                if ((line[i] != null) || (rel.line[i] != null)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return line.length;
    }

    /**
     * Subset test.
     * @param  sub  relation to compare with.
     * @return true, if every element of sub is element of this
     */
    public boolean contains(IntBitRelation sub) {
        int i;

        if (line.length != sub.line.length) {
            return false;
        }

        for (i = 0; i < line.length; i++) {
            if (line[i] == null) {
                if (sub.line[i] != null) {
                    return false;
                }
            } else {
                if ((sub.line[i] != null) &&
                    !line[i].containsAll(sub.line[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Transitive closure. */
    public void transitiveClosure() {
        IntBitRelation next;

        while (true) {
            next = new IntBitRelation(getMax());
            next.composeRightLeft(this, this);
            if (contains(next)) {
                return;
            }
            addAll(next);
        }
    }

    //-------------------------------------------------------------

    /**
     * Returns a String representation.
     * @return  string representation
     */
    @Override
    public String toString() {
        StringBuilder buf;
        int i;

        buf = new StringBuilder();
        for (i = 0; i < line.length; i++) {
            if (line[i] != null) {
                buf.append(i).append(": ").append(line[i].toString()).append('\n');
            }
        }
        return buf.toString();
    }
}
