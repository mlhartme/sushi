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

import java.lang.reflect.Array;

/**
 * Additional functionality over java.util.Arrays.
 */

public class Arrays {
    public static Object[] append(Class<?> cl, Object[] left, Object[] right) {
        Object[] result;

        result = (Object[]) Array.newInstance(cl, left.length + right.length);
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);
        return result;
    }

    public static Object[] cons(Class<?> cl, Object left, Object[] right) {
        Object[] result;

        result = (Object[]) Array.newInstance(cl, 1 + right.length);
        result[0] = left;
        System.arraycopy(right, 0, result, 1, right.length);
        return result;
    }

    public static Object[] pair(Class<?> cl, Object left, Object right) {
        Object[] result;

        result = (Object[]) Array.newInstance(cl, 2);
        result[0] = left;
        result[1] = right;
        return result;
    }

    /**
     * Return an array class object with component as component type.
     * The API description for java.lang.Class (Java 2) mentions
     * that forName can be called with an Array name, but neither
     * Class.forName("java.lang.String[]") nor
     * Class.forName("[java.lang.String") returns an array class!?
     */
    public static Class<?> getArrayClass(Class<?> component) {
        return Array.newInstance(component, 0).getClass();
    }
}
