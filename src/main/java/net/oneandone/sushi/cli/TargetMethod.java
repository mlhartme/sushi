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

import net.oneandone.sushi.metadata.Schema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

public class TargetMethod extends Target {
    public static TargetMethod create(Schema schema, Object context, Method method) {
        Parameter[] formals;

        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(method + ": static not allowed");
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException(method + ": public expected");
        }
        formals = method.getParameters();
        if (formals.length != 1) {
            throw new IllegalArgumentException("1 argument expected");
        }
        return new TargetMethod(schema, formals[0].getParameterizedType(), context, method);
    }
    
    //--

    private final Object context;
    private final Method method;
    
    public TargetMethod(Schema schema, java.lang.reflect.Type type, Object context, Method method) {
        super(schema, type);
        this.context = context;
        this.method = method;
    }

    public boolean before() {
        return context != null;
    }

    @Override
    public void doSet(Object obj, Object value) {
        Throwable cause;
        
        try {
            method.invoke(context == null ? obj : context, value);
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
