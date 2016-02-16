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

import net.oneandone.sushi.metadata.SimpleType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class TargetMethodIterated extends Target {
    private final Object context;
    private final Method method;

    public TargetMethodIterated(boolean list, SimpleType component, Object context, Method method) {
        super(list, component);
        this.context = context;
        this.method = method;
    }

    public boolean before() {
        return context != null;
    }

    @Override
    public void doSet(Object dest, Object value) {
        List<Object> lst;
        Throwable cause;

        lst = (List) value;
        try {
            for (Object item : lst) {
                method.invoke(context == null ? dest : context, item);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(value + ":" + value.getClass(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            cause = e.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            if (cause instanceof ArgumentException) {
                throw (ArgumentException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("unexpected exception" , cause);
        }
    }
}
