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

import java.util.List;
import java.util.Random;

public class Invoker extends Thread {
    public static void runAll(int threadCount, int stepCount, List<Function> functions) throws Exception {
        Invoker[] threads;
        Result result;

        threads = new Invoker[threadCount];
        result = new Result(100);
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Invoker(i + 1, functions, stepCount, result);
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        result.fail();
    }

    private final int id;
    private final Result result;
    private final Random random;
    private final List<Function> functions;
    private final int stepCount;

    public Invoker(int id, List<Function> functions, int stepCount, Result result) {
        this.id = id;
        this.result = result;
        this.random = new Random();
        this.functions = functions;
        this.stepCount = stepCount;
    }

    public void run() {
        Function function;
        long started;
        Throwable throwable;

        for (int i = 0; i < stepCount; i++) {
            function = functions.get(random.nextInt(functions.size()));
            started = System.currentTimeMillis();
            try {
                function.invoke();
                throwable = null;
            } catch (Throwable e) {
                throwable = e;
            }
            if (result.add(id, function, throwable, started, System.currentTimeMillis())) {
                return;
            }
        }
    }
}
