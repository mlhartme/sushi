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

package net.sf.beezle.sushi.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class Reflect {
    public static String resourceName(Class<?> clazz) {
        return "/" + clazz.getName().replace('.', '/') + ".class";        
    }
    
    public static String importName(Class<?> clazz) {
        String name;

        name = clazz.getName();
        return name.replace('$', '.');
    }

    private static final List<?> PRIMITIVE_TYPES = Arrays.asList(
        Void.TYPE, Boolean.TYPE, Byte.TYPE, Character.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE
    );
    
    private static final List<?> WRAPPER_TYPES = Arrays.asList(
        Void.class, Boolean.class, Byte.class, Character.class, Integer.class, Long.class, Float.class, Double.class
    );
    
    public static Class<?> getWrapper(Class<?> primitive) {
        int idx;

        idx = PRIMITIVE_TYPES.indexOf(primitive);
        if (idx == -1) {
            return null;
        } else {
            return (Class<?>) WRAPPER_TYPES.get(idx);
        }
    }

    public static Class<?> getPrimitive(Class<?> wrapper) {
        int idx;

        idx = WRAPPER_TYPES.indexOf(wrapper);
        if (idx == -1) {
            return null;
        } else {
            return (Class<?>) PRIMITIVE_TYPES.get(idx);
        }
    }

    public static String getSimpleName(Class<?> cls) {
        String name;
        int idx;

        name = Reflect.importName(cls);
        idx = name.lastIndexOf('.');
        return name.substring(idx + 1); // ok for -1 :-)
    }

    public static Method lookup(Class<?> task, String rawName) {
        return lookup(task, rawName, new Class[] {});
    }

    public static Method lookup(Class<?> task, String rawName, Class<?> arg) {
        return lookup(task, rawName, new Class[] { arg });
    }

    public static Method lookup(Class<?> task, String rawName, Class<?> ... args) {
        Method[] methods;
        int i;
        Method m;
        Method found;

        methods = task.getMethods();
        found = null;
        for (i = 0; i < methods.length; i++) {
            m = methods[i];
            if (equals(args, m.getParameterTypes())
                    && m.getName().equalsIgnoreCase(rawName)) {
                if (found != null) {
                    throw new IllegalArgumentException("duplicate method: " + rawName);
                }
                found = m;
            }
        }
        return found;
    }

    public static boolean equals(Class<?>[] left, Class<?>[] right) {
        int i;

        if (left.length != right.length) {
            return false;
        }
        for (i = 0; i < left.length; i++) {
            if (!left[i].equals(right[i])) {
                return false;
            }
        }
        return true;
    }
    
    //--
    
    public static <T extends Enum<?>> T[] getValues(Class<T> clazz) {
        Method m;
        
        try {
            m = clazz.getDeclaredMethod("values");
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        m.setAccessible(true);
        try {
            return (T[]) m.invoke(null);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static boolean matches(Class<?>[] formals, Object[] actuals) {
        int max;
        Class<?> formal;
        Object actual;

        max = formals.length;
        if (actuals.length != max) {
            return false;
        }
        for (int i = 0; i < max; i++) {
            formal = formals[i];
            actual = actuals[i];
            if (!formal.isInstance(actual)) {
                if (formal.isPrimitive()) {
                    formal = Reflect.getWrapper(formal);
                    if (!formal.isInstance(actual)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

}
