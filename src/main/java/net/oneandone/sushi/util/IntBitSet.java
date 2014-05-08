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

import java.io.Serializable;
import java.util.List;

/**
 * <p>A set of non-negative int values. IntBitSet is particularly usefull
 * to store array indexes. IntBitSet is similar to BitSet, but with
 * additional functionality, mainly subset tests and enumeration of elements.
 * Method names follow the style of collections rather than BitSet
 * because usually collections are better known. </p>
 *
 * <p>Tuned for speed. Each int value is represented by a staticallly
 * chosen bit in a data array. Thus the space required for a set
 * determined by largest element. This is memory-consuming. However.
 * if the set stores array indexes and memory is available for a big
 * array, there is probably enough memory to store big indexes as well.
 * </p>
 *
 * <p>The method names follow java.util.BitSet instead of java.util.Set.
 * The main reasons are: 1. IntBitSet stores primitive values instead of
 * Objects. 2. IntBitSet is to limited to be called a collection, because
 * adding large int values result in a huge memory consumption. </p>
 *
 * <p>Java specifies that the right argument of shift operations is
 * implicitly bit-anded with 0x1f. I rely on this and don't mask any
 * indexes. </p>
 */

public class IntBitSet implements IntCollection, Serializable {
    public static IntBitSet with(int ... elements) {
        IntBitSet result;

        result = new IntBitSet();
        for (int e : elements) {
            result.add(e);
        }
        return result;
    }

    /** Number to shift in order to switch from an index to an
        element. */
    private static final int SHIFT = 5;

    /** Bit mask to obtain SHIFT bits. */
    private static final int MASK = 0x1f;

    /** Number of components data is grown if necessary. */
    private static final int GROW = 8;


    /** Storage for the elements. */
    private int[] data;

    //-------------------------------------------------------------

    /** Creates an empty set. */
    public IntBitSet() {
        data = new int[GROW];
    }

    /** Copy constructor. */
    public IntBitSet(IntBitSet set) {
        data = new int[set.data.length];
        System.arraycopy(set.data, 0, data, 0, data.length);
    }

    public IntBitSet(int[] data) {
        this.data = data;
    }


    //-------------------------------------------------------------------
    // iteration

    /**
     * Gets the first element of the set.
     * @return   first element of the set, -1 if the set is empty
     */
    public int first() {
        int val;
        int i;

        for (i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                val = data[i];
                i <<= SHIFT;
                while ((val & 1) == 0) {
                    val >>>= 1;
                    i++;
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the last element of the set.
     * @return   last element of the set, -1 if the set is empty
     */
    public int last() {
        int val;
        int i;

        for (i = data.length - 1; i >= 0; i--) {
            if (data[i] != 0) {
                val = data[i] >>> 1;
                i <<= SHIFT;
                while (val != 0) {
                    val >>>= 1;
                    i++;
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the element following ele.
     * @param   ele  the element to start with
     * @return  the element following ele, -1 if nothing follows
     */
    public int next(int ele) {
        int idx, bit, val;

        idx = ele >> SHIFT;
        bit = ele & MASK;
        val = data[idx] >>> bit;

        do {
            val >>>= 1;
            bit++;
            if (val == 0) {
                idx++;
                if (idx == data.length) {
                    return -1;
                }
                val = data[idx];
                bit = 0;
            }
        } while ((val & 1) == 0);
        return (idx << SHIFT) + bit;
    }

    public int prev(int ele) {
        // TODO: very expensive
        while (ele > 0) {
            ele--;
            if (contains(ele)) {
                return ele;
            }
        }
        return -1;
    }

    //-----------------------------------------------------------------------
    // access elements

    /**
     * Lookup a specific element.
     * @param   ele  element to lookup
     * @return  true, if ele is contained in the set
     */
    public boolean contains(int ele) {
        int idx;

        idx = ele >> SHIFT;
        if (idx >= data.length) {
            return false;
        }
        return (data[idx] & (1 << ele)) != 0;
    }

    public boolean containsSome(IntBitSet rest) {
        int ele;

        for (ele = first(); ele != -1; ele = next(ele)) {
            if (rest.contains(ele)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add an element.
     * @param  ele  the element to add
     */
    public void add(int ele) {
        int idx;

        idx = ele >> SHIFT;
        if (idx >= data.length) {
            resize(idx + 1 + GROW);
        }
        data[ele >> SHIFT] |= (1 << ele);
    }

    /**
     * Add all elements in the indicated range. Nothing is set for first &gt; last.
     * @param  first   first element to add
     * @param  last    last element to add
     */
    public void addRange(int first, int last) {
        int ele;

        for (ele = first; ele <= last; ele++) {
            add(ele);
        }
    }

    /**
     * Remove an element from the set.
     * @param  ele  element to remove
     */
    public void remove(int ele) {
        int idx;

        idx = ele >> SHIFT;
        if (idx < data.length) {
            data[idx] &= ~(1 << ele);
        }
    }

    /**
     * Remove all elements in the indicated range.
     * @param  first  first element to remove
     * @param  last   last element to remove
     */
    public void removeRange(int first, int last) {
        int ele;

        for (ele = first; ele <= last; ele++) {
            remove(ele);
        }
    }

    /**
     * Remove all elements.
     */
    public void clear() {
        int i;

        for (i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }

    //-----------------------------------------------------------------------
    // comparison

    @Override
    public int hashCode() {
        return size();
    }

    /**
     * Comparison.
     * @param   obj  the Object to compare with
     * @return  true, if obj is a set and contains exactly the
     *          same elements as this set
     */
    @Override
    public boolean equals(Object obj) {
        IntBitSet set;
        int i;
        int[] x;
        int[] y;

        if (!(obj instanceof IntBitSet)) {
            return false;
        }

        set = (IntBitSet) obj;
        if (data.length < set.data.length) {
            x = data;
            y = set.data;
        } else {
            x = set.data;
            y = data;
        }

        for (i = 0; i < x.length; i++) {
            if (x[i] != y[i]) {
                return false;
            }
        }
        for ( ; i < y.length; i++) {
            if (y[i] != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * The subset test.
     * @param   set  the set to be compared with
     * @return  true, if the argument is contained
     */
    public boolean containsAll(IntBitSet set) {
        int i, end;

        if (data.length < set.data.length) {
            end = data.length;
            for (i = end; i < set.data.length; i++) {
                if (set.data[i] != 0) {
                    return false;
                }
            }
        } else {
            end = set.data.length;
        }

        for (i = 0; i < end; i++) {
            // any bits in set where this has none?  -> false
            if ((set.data[i] & ~data[i])!= 0) {
                return false;
            }
        }
        return true;
    }

    //----------------------------------------------------------------------
    // set operations

    /**
     * Computes the intersection. The result is stored in this set.
     * @param  set  the set to be combined with */
    public void retainAll(IntBitSet set) {
        int i;

        if (set.data.length > data.length) {
            resize(set.data.length);
        }
        for (i = 0; i < set.data.length; i++) {
            data[i] &= set.data[i];
        }
        for ( ; i < data.length; i++) {
            data[i] = 0;
        }
    }

    /**
     * Computes the set difference. The result is stored in this set.
     * @param   set   the set to be combined with
     */
    public void removeAll(IntBitSet set) {
        int i, end;

        end = (set.data.length < data.length)? set.data.length : data.length;
        for (i = 0; i < end; i++) {
            data[i] &= ~set.data[i];
        }
    }

    /**
     * Computes the union. The result is stored in this set.
     * @param   set   the set to be combined with
     */
    public void addAll(IntBitSet set) {
        int i;

        if (set.data.length > data.length) {
            resize(set.data.length);
        }

        for (i = 0; i < set.data.length; i++) {
            data[i] |= set.data[i];
        }
    }

    public void addAllSets(IntBitSet[] related) {
        int ele;

        for (ele = first(); ele != -1; ele = next(ele)) {
            addAll(related[ele]);
        }
    }


    //----------------------------------------------------------------
    // misc

    /**
     * Counts the elements.
     * @return  number of elements in this set
     */
    public int size() {
        int ele, count;

        count = 0;
        for (ele = first(); ele != -1; ele = next(ele)) {
            count++;
        }
        return count;
    }

    /**
     * Tests for the empty set.
     * @return  true, if the set contains no elements
     */
    public boolean isEmpty() {
        return first() == -1;
    }

    /**
     * Creates an array with all elements of the set.
     * @return   array with all elements of the set
     */
    public int[] toArray() {
        int i, ele;
        int[] result;

        result = new int[size()];
        for (ele = first(), i = 0; ele != -1; ele = next(ele), i++) {
            result[i] = ele;
        }
        return result;
    }

    /**
     * Changes data to have data.length >= s components. All data
     * components that fit into the new size are preserved.
     * @param   s   new data size wanted
     */
    private void resize(int s) {
        int[] n;
        int count;

        n = new int[s];
        count = (s < data.length)? s : data.length;
        System.arraycopy(data, 0, n, 0, count);
        data = n;
    }

    //-----------------------------------------------------------------
    // output

    /**
     * Returns a String representation of the set.
     * @return  a String listing all elements
     */
    @Override
    public String toString() {
        StringBuilder buffer;
        int ele;

        buffer = new StringBuilder("{");
        for (ele = first(); ele != -1; ele = next(ele)) {
            buffer.append(' ').append(ele);
        }
        buffer.append(" }");
        return buffer.toString();
    }

    /**
     * Returns a String representation with each element
     * represented by a String taken from a Symboltable.
     * @param   symbols  supplies the symbols to represent the elements
     *                   of the set; a RuntimeException is thrown if an
     *                   element is not found here
     * @return  a String listing all elements of the set
     */
    public String toString(List<?> symbols) {
         StringBuilder buf;
         int ele;
         int max;

         max = symbols.size();
         buf = new StringBuilder("{");
         for (ele = first(); ele != -1; ele = next(ele)) {
             buf.append(" ");
             if (ele < max) {
                 buf.append(symbols.get(ele));
             } else {
                 buf.append('<').append(ele).append('>');
             }
         }
         buf.append(" }");
         return buf.toString();
    }
}
