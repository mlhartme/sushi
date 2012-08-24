/**
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
package net.sf.beezle.sushi.fs.multi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class Function {
    public static void forTarget(String targetName, Object target, List<Function> result) {
        for (Method method : target.getClass().getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                result.add(new Function(targetName + "." + method.getName() + "()", target, method));
            }
        }
    }

    private final String name;
    private final Object target;
    private final Method method;

    public Function(String name, Object target, Method method) {
        this.name = name;
        this.target = target;
        this.method = method;
    }

    public void invoke() throws Throwable {
        try {
            method.invoke(target);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public String toString() {
        return name;
    }
}
