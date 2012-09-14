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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class Function {
    public static void forTarget(String targetName, Object target, List<Function> result) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                result.add(new Function(targetName + "." + method.getName() + "()", target, method));
            }
        }
    }

    private final String name;
    private final Object target;
    private final Method method;

    public Function(String name, Object target, Method method) {
        this.name = name;
        this.target = target;
        this.method = method;
    }

    public void invoke() throws Throwable {
        try {
            method.invoke(target);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String toString() {
        return name;
    }
}
