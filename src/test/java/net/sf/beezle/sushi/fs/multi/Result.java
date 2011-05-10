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

import net.sf.beezle.sushi.util.Strings;
import net.sf.beezle.sushi.util.Util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Result {
    private int next;
    private final int[] ids;
    private final Method[] methods;
    private final Throwable[] throwables;
    private final long[] starts;
    private final long[] ends;
    private boolean complete;

    public Result(int size) {
        next = 0;
        ids = new int[size];
        methods = new Method[size];
        starts = new long[size];
        ends = new long[size];
        throwables = new Throwable[size];
        complete = false;
    }

    /** @return true if the calling thread should complete */
    public synchronized boolean add(int id, Method method, Throwable throwable, long start, long end) {
        ids[next] = id;
        methods[next] = method;
        starts[next] = start;
        ends[next] = end;
        throwables[next] = throwable;
        next = (next + 1) % starts.length;
        if (!complete && throwable != null) {
            complete = true;
        }
        return complete;
    }


    public String toString() {
        long firstStart;
        int i, count;
        StringBuilder result;

        result = new StringBuilder();
        firstStart = firstStart();
        for (i = startIndex(), count = size(); count > 0; i = (i + 1) % methods.length, count--) {
            result.append(starts[i] - firstStart);
            result.append('-');
            result.append(ends[i] - firstStart);
            result.append('@');
            result.append(ids[i]);
            result.append(": ");
            result.append(methods[i].getName());
            if (throwables[i] != null) {
                result.append(":");
                result.append(Strings.indent(Util.toString(throwables[i]), "  "));
            }
            result.append('\n');
        }
        return result.toString();
    }

    public int startIndex() {
        return methods[next] != null ? next : 0;
    }

    public int size() {
        return methods[next] != null ? methods.length : next;
    }

    public void fail() {
        List<Throwable> lst;

        lst = new ArrayList<Throwable>();
        for (int i = 0; i < throwables.length; i++) {
            if (throwables[i] != null) {
                lst.add(throwables[i]);
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

    public long firstStart() {
        long result;

        result = Long.MAX_VALUE;
        for (int i = 0; i < methods.length; i++) {
            if (methods[i] != null) {
                result = Math.min(result, starts[i]);
            }
        }
        return result;
    }
}
