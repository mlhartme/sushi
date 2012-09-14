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
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.Type;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ArgumentMethod extends Argument {
    public static ArgumentMethod create(String name, Schema metadata, Method method) {
        Class<?>[] formals;
        Type type;
        
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(method + ": static not allowed");
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException(method + ": public expected");
        }
        formals = method.getParameterTypes();
        if (formals.length != 1) {
            throw new IllegalArgumentException("1 argument expected");
        }
        type = metadata.type(formals[0]);
        return new ArgumentMethod(name, type instanceof SimpleType ? (SimpleType) type : null, method);
    }
    
    //--
    
    private final Method method;
    
    public ArgumentMethod(String name, SimpleType simple, Method method) {
        super(name, simple);
        this.method = method;
    }

    @Override
    public void set(Object obj, Object value) {
        Throwable cause;
        
        try {
            method.invoke(obj, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(getName() + ": " + value + ":" + value.getClass(), e);
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
                throw new RuntimeException(getName(), cause);
            }
            throw new RuntimeException("unexpected exception" , cause);
        }
    }
}
