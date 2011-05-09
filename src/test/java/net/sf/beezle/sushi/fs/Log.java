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

package net.sf.beezle.sushi.fs;

public class Log {
    private Step[] steps;
    private Exception[] exceptions;
    private long[] starteds;
    private long[] endeds;
    private int next;

    public Log(int size) {
        starteds = new long[size];
        endeds = new long[size];
        steps = new Step[size];
        exceptions = new Exception[size];
    }

    public synchronized void ok(Step step, long started) {
        add(step, null, started);
    }

    public synchronized void failed(Step step, Exception exception, long started) {
        add(step, exception, started);
    }

    private void add(Step step, Exception exception, long started) {
        starteds[next] = started;
        endeds[next] = System.currentTimeMillis();
        steps[next] = step;
        exceptions[next] = exception;
        next = (next + 1) % steps.length;
    }
}
