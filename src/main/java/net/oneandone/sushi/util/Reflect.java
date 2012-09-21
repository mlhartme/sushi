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
package net.oneandone.sushi.util;

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
        } catch (SecurityException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        m.setAccessible(true);
        try {
            return (T[]) m.invoke(null);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
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
