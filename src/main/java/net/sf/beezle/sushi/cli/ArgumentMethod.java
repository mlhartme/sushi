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

package net.sf.beezle.sushi.cli;

import net.sf.beezle.sushi.metadata.Schema;
import net.sf.beezle.sushi.metadata.SimpleType;
import net.sf.beezle.sushi.metadata.Type;

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
