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

public class RepeatThread extends Thread {
    public static void runAll(Step step, int parallel, int count) throws Exception {
        RepeatThread[] threads;

        threads = new RepeatThread[parallel];
        for (int i = 0; i < parallel; i++) {
            threads[i] = new RepeatThread(step, count);
        }
        for (int i = 0; i < parallel; i++) {
            threads[i].start();
        }
        for (int i = 0; i < parallel; i++) {
            threads[i].finish();
        }
    }
    private final Step step;
    private final int count;
    private Exception exception;

    public RepeatThread(Step step, int count) {
        this.step = step;
        this.count = count;
        this.exception = null;
    }

    public void run() {
        for (int i = 0; i < count; i++) {
            try {
                step.invoke();
            } catch (Exception e) {
                exception = e;
            }
        }
    }

    public void finish() throws Exception {
        join();
        if (exception != null) {
            throw exception;
        }
    }
}
