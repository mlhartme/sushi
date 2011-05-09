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

import java.util.Random;

public class RandomStepThread extends Thread {
    public static void runAll(int threadCount, int stepCount, Step... steps) throws Exception {
        RandomStepThread[] threads;
        Result result;

        threads = new RandomStepThread[threadCount];
        result = new Result(100);
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new RandomStepThread(steps, stepCount, result);
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        result.fail();
    }

    private final Result result;
    private final Random random;
    private final Step[] steps;
    private final int stepCount;

    public RandomStepThread(Step[] steps, int stepCount, Result result) {
        this.result = result;
        this.random = new Random();
        this.steps = steps;
        this.stepCount = stepCount;
    }

    public void run() {
        Step step;
        long started;
        Exception exception;

        for (int i = 0; i < stepCount; i++) {
            step = steps[random.nextInt(steps.length)];
            started = System.currentTimeMillis();
            try {
                step.invoke();
                exception = null;
            } catch (Exception e) {
                exception = e;
            }
            if (result.add(step, exception, started, System.currentTimeMillis())) {
                return;
            }
        }
    }
}
