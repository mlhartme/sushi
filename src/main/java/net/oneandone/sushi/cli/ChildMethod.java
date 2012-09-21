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
package net.oneandone.sushi.cli;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ChildMethod {
    /** @param m must not throw checked exceptions; it's expected to have no real side effects. */
    public static void check(Method m) {
        if (!Modifier.isPublic(m.getModifiers())) {
            throw new IllegalArgumentException(m.getName());
        }
        if (Modifier.isStatic(m.getModifiers())) {
            throw new IllegalArgumentException(m.getName());
        }
        if (m.getParameterTypes().length != 0) {
            throw new IllegalArgumentException(m.getName());
        }
        if (!Object.class.isAssignableFrom(m.getReturnType())) {
            throw new IllegalArgumentException(m.getName());
        }
    }
    
    //--
    
    private final String name;
    private final Method meth;
    
    public ChildMethod(String name, Method meth) {
        check(meth);
        
        this.name = name;
        this.meth = meth;
    }
    
    public String getName() {
        return name;
    }

    /** @throws ArgumentException for every checked exception thrown by the underlying method */
    public Object invoke(Object obj) throws ArgumentException {
        Throwable cause;
        
        try {
            return meth.invoke(obj);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ArgumentException(cause.getMessage(), cause);
        }
    }
}
