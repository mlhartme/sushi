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

package com.oneandone.sushi.util;


public class Misc {
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
}

