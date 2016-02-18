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
    public static Target create(boolean iterated, Schema schema, Method method) {
        Parameter[] formals;
        java.lang.reflect.Type type;

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
        type = formals[0].getParameterizedType();
        if (iterated) {
            return new TargetMethodIterated(true, schema.simple((Class) type), method);
        } else {
            return new TargetMethod(schema, type, method);
        }
    }
    
    //--

    private final Method method;
    
    public TargetMethod(Schema schema, java.lang.reflect.Type type, Method method) {
        super(schema, type);
        this.method = method;
    }

    public boolean before() {
        return false;
    }

    @Override
    public void doSet(Object dest, Object value) {
        Throwable cause;
        
        try {
            method.invoke(dest, value);
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
