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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Misc static utility methods.
 */
public class Util {
    /**
     * @return absolute resource name
     */
    public static String getPackageResourceName(Class<?> clazz) {
        String name;
        int idx;

        name = clazz.getName();
        idx = name.lastIndexOf('.');
        if (idx == -1) {
            throw new RuntimeException(name);
        }
        name = name.substring(0, idx);
        return "/" + name.replace('.', '/');
    }

    public static boolean eq(Object a, Object b) {
        return (a == null)? b == null : a.equals(b);
    }

    public static int find(List<?>[] as, Object a) {
        int i;
        int j;
        int max;
        List<?> current;

        for (i = 0; i < as.length; i++) {
            current = as[i];
            max = current.size();
            for (j = 0; j < max; j++) {
                if (current.get(j) == a) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String toString(Throwable e) {
        StringWriter dest;
        PrintWriter pw;

        dest = new StringWriter();
        pw = new PrintWriter(dest);
        e.printStackTrace(pw);
        pw.close();
        return dest.toString();
    }

    public static String toString(byte[] bytes) {
        StringBuilder result;

        result = new StringBuilder();
        for (byte b : bytes) {
            if ((b >= ' ' && b <= 'z') || b == 10) {
                result.append((char) b);
            } else {
                result.append('[').append((int) b).append(']');
            }
        }
        return result.toString();
    }
}
