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

/**
 * List of int elements. Similar to java.lang.List or
 * java.util.ArrayList, but it stores primitive int values.
 * Generic collections for primitive types whould remove the
 * need for IntArrayList. I implemented only those methods
 * that I acually need.
 */

public class IntArrayList implements IntCollection, Serializable {
    /** Storage for elements. */
    private int[] data;

    /** Number of data elements actually used. */
    private int size;

    public IntArrayList(int size, int[] data) {
        this.size = size;
        this.data = data;
    }

    public IntArrayList(int[] init) {
        size = init.length;
        data = new int[size];
        System.arraycopy(init, 0, data, 0, size);
    }
    //--------------------------------------------------------------

    /** Creates a new empty List, initial size is 32. */
    public IntArrayList() {
        this(32);
    }

    public IntArrayList(int initialSize) {
        data = new int[initialSize];
        size = 0;
    }

    /**
     * Copy constructor.
     * @param  orig  List that supplies the initial elements for
     *               the new List.
     */
    public IntArrayList(IntArrayList orig) {
        data = new int[orig.data.length];
        size = orig.size;
        System.arraycopy(orig.data, 0, data, 0, size);
    }

    //-----------------------------------------------------------------

    @Override
    public int hashCode() {
        return size();
    }
    
    @Override
    public boolean equals(Object obj) {
        IntArrayList operand;
        int i;

        if (obj instanceof IntArrayList) {
            operand = (IntArrayList) obj;
            if (size == operand.size) {
                for (i = 0; i < size; i++) {
                    if (data[i] != operand.data[i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Gets an element from the List.
     * @param   idx  index of the element asked for
     * @return  selected element
     */
    public int get(int idx) {
        return data[idx];
    }

    /**
     * Replaces an element in the List.
     * @param  ele  new element
     * @param  idx  index of the element to be replaced
     */
    public void set(int idx, int ele) {
        data[idx] = ele;
    }

    public void ensureCapacity(int min) {
        int[] tmp;
        int old;
        int capacity;

        old = data.length;
        if (min > old) {
            tmp = data;
            capacity = (old * 5) / 3 + 1;
            if (capacity < min) {
                capacity = min;
            }
            data = new int[capacity];
            System.arraycopy(tmp, 0, data, 0, size);
        }
    }

    /**
     * Adds an element to the List. All following elements
     * are moved up by one index.
     * @param  idx  where to insert the new element
     * @param  ele  new element
     */
    public void add(int idx, int ele) {
        ensureCapacity(size + 1);
        System.arraycopy(data, idx, data, idx + 1, size - idx);
        data[idx] = ele;
        size++;
    }

    /**
     * Adds an element to the end of the List.
     * @param  ele  new element
     */
    public void add(int ele) {
        ensureCapacity(size + 1);
        data[size++] = ele;
    }

    public void addAll(IntArrayList op) {
        ensureCapacity(size + op.size);
        System.arraycopy(op.data, 0, data, size, op.size);
        size += op.size;
    }


    /**
     * Removes an element from the List. All following elements
     * are moved down by one index.
     * @param  idx  index of the element to remove
     */
    public void remove(int idx) {
        size--;
        System.arraycopy(data, idx + 1, data, idx, size - idx);
    }

    /**
     * Removes all elements.
     */
    public void clear() {
        size = 0;
    }

    /**
     * Searches an element.
     * @param   ele  element to look for
     * @return  index of the first element found; -1 if nothing was found
     */
    public int indexOf(int ele) {
        int i;

        for (i = 0; i < size; i++) {
            if (data[i] == ele) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(int ele) {
        return indexOf(ele) != -1;
    }

    /**
     * Returns the number of elements in the List.
     * @return number of elements
     */
    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Creates an array with all elements of the List.
     * @return  the array requested
     */
    public int[] toArray() {
        return toArray(new int[size]);
    }

    public int[] toArray(int[] result) {
        if (result.length < size) {
            result = new int[size];
        }
        System.arraycopy(data, 0, result, 0, size);
        return result;
    }

    //-----------------------------------------------------------------

    /**
     * Returns a string representation.
     * @return string representation
     */
    @Override
    public String toString() {
        StringBuilder buffer;
        int i, max;

        max = size();
        buffer = new StringBuilder();
        for (i = 0; i < max; i++) {
            buffer.append(' ');
            buffer.append(get(i));
        }
        return buffer.toString();
    }
}
