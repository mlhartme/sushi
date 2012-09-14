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
package net.oneandone.sushi.fs.multi;

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
