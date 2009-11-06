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

package de.ui.sushi.cli;

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
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
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
