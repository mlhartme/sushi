/*
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

package net.sf.beezle.sushi.fs.multi;

import java.util.ArrayList;
import java.util.List;

public class Result {
    private int next;
    private Step[] steps;
    private Exception[] exceptions;
    private long[] starts;
    private long[] ends;
    private boolean complete;

    public Result(int size) {
        next = 0;
        steps = new Step[size];
        starts = new long[size];
        ends = new long[size];
        exceptions = new Exception[size];
        complete = false;
    }

    /** @return true if the calling thread should complete */
    public synchronized boolean add(Step step, Exception exception, long start, long end) {
        steps[next] = step;
        starts[next] = start;
        ends[next] = end;
        exceptions[next] = exception;
        next = (next + 1) % starts.length;
        if (!complete && exception != null) {
            complete = true;
        }
        return complete;
    }


    public String toString() {
        int i, count;
        StringBuilder result;

        result = new StringBuilder();
        for (i = startIndex(), count = size(); count > 0; i = (i + 1) % steps.length, count--) {
            result.append(steps[i]);
            if (exceptions[i] != null) {
                result.append(": ");
                result.append(exceptions[i].getMessage());
            }
            result.append('\n');
        }
        return result.toString();
    }

    public int startIndex() {
        return steps[next] != null ? next : 0;
    }

    public int size() {
        return steps[next] != null ? steps.length : next;
    }

    public void fail() {
        List<Exception> lst;

        lst = new ArrayList<Exception>();
        for (int i = 0; i < exceptions.length; i++) {
            if (exceptions[i] != null) {
                lst.add(exceptions[i]);
            }
        }
        switch (lst.size()) {
            case 0:
                return;
            case 1:
                throw new RuntimeException("1 failure:\n" + toString(), lst.get(0));
            default:
                throw new RuntimeException("multiple failures\n" + toString());
        }
    }
}