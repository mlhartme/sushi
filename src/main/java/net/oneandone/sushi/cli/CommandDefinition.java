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

public class CommandDefinition {
    public static CommandDefinition create(CommandParser parser, String name, Method method) {
        Class<?> returnType;

        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException(method + ": static not allowed");
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException(method + ": public expected");
        }
        if (method.getParameterTypes().length != 0) {
            throw new IllegalArgumentException("unexpected arguments");
        }
        returnType = method.getReturnType();
        if (Void.TYPE.equals(returnType) || Integer.TYPE.equals(returnType)) {
            return new CommandDefinition(parser, name, method);
        } else {
            throw new IllegalArgumentException("unsupported return type: " + returnType);
        }
    }

    //--

    private final CommandParser parser;
    private final String name;
    private final Method method;

    public CommandDefinition(CommandParser parser, String name, Method method) {
        this.parser = parser;
        this.name = name;
        this.method = method;
    }

    public CommandParser getParser() {
        return parser;
    }

    public String getName() {
        return name;
    }

    public int invoke(Object obj) {
        Throwable cause;
        Object result;
        
        try {
            result = method.invoke(obj);
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
                throw new RuntimeException(name, cause);
            }
            throw new RuntimeException("unexpected exception" , cause);
        }
        if (result instanceof Integer) {
            return (Integer) result;
        } else {
            return 0;
        }
    }
}
