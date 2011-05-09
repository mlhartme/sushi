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

import java.util.Random;

public class RepeatThread extends Thread {
    public static void runAll(int parallel, int count, Step ... steps) throws Exception {
        RepeatThread[] threads;
        Log log;

        threads = new RepeatThread[parallel];
        log = new Log(100);
        for (int i = 0; i < parallel; i++) {
            threads[i] = new RepeatThread(steps, count, log);
        }
        for (int i = 0; i < parallel; i++) {
            threads[i].start();
        }
        for (int i = 0; i < parallel; i++) {
            threads[i].finish();
        }
    }

    private final Log log;
    private final Random random;
    private final Step[] steps;
    private final int count;
    private Exception exception;

    public RepeatThread(Step[] steps, int count, Log log) {
        this.log = log;
        this.random = new Random();
        this.steps = steps;
        this.count = count;
        this.exception = null;
    }

    public void run() {
        Step step;
        long started;
        Exception exception;

        for (int i = 0; i < count; i++) {
            step = steps[random.nextInt(steps.length)];
            started = System.currentTimeMillis();
            try {
                step.invoke();
                exception = null;
            } catch (Exception e) {
                exception = e;
            }
            if (log.add(step, exception, started, System.currentTimeMillis())) {
                return;
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
