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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Invoker extends Thread {
    public static void runAll(int threadCount, int stepCount, Object target) throws Exception {
        Invoker[] threads;
        Result result;

        threads = new Invoker[threadCount];
        result = new Result(100);
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Invoker(i + 1, target, getMethods(target), stepCount, result);
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].join();
        }
        result.fail();
    }

    private static List<Method> getMethods(Object target) {
        List<Method> result;

        result = new ArrayList<Method>();
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                result.add(method);
            }
        }
        return result;
    }
    
    private final int id;
    private final Result result;
    private final Random random;
    private final Object target;
    private final List<Method> methods;
    private final int stepCount;

    public Invoker(int id, Object target, List<Method> methods, int stepCount, Result result) {
        this.id = id;
        this.result = result;
        this.random = new Random();
        this.target = target;
        this.methods = methods;
        this.stepCount = stepCount;
    }

    public void run() {
        Method method;
        long started;
        Throwable throwable;

        for (int i = 0; i < stepCount; i++) {
            method = methods.get(random.nextInt(methods.size()));
            started = System.currentTimeMillis();
            try {
                method.invoke(target);
                throwable = null;
            } catch (InvocationTargetException e) {
                throwable = e.getTargetException();
            } catch (Exception e) {
                throwable = e;
            }
            if (result.add(id, method, throwable, started, System.currentTimeMillis())) {
                return;
            }
        }
    }
}
