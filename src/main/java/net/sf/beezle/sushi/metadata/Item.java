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

package net.sf.beezle.sushi.metadata;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** An item in a complex type. TODO: rename to field? */
public abstract class Item<T> {
    protected static Method lookup(Class type, String name) {
        Method[] methods;
        int i;
        Method found;
        Method tmp;
        
        methods = type.getMethods();
        found = null;
        
        for (i = 0; i < methods.length; i++) {
            tmp = methods[i];
            if (tmp.getName().equalsIgnoreCase(name)) {
                if (found != null) {
                    throw new IllegalArgumentException("ambiguous: " + name);
                }
                found = tmp;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("not found: " + name);
        }
        return found;
    }

    protected static void checkSetter(Class type, Method setter) {
        if (setter.getParameterTypes().length != 1) {
            fail(setter);
        }
        if (!setter.getParameterTypes()[0].equals(type)) {
            fail(setter);
        }
        if (!setter.getReturnType().equals(Void.TYPE)) {
            fail(setter);
        }
        check(setter);
    }

    protected static void checkGetter(Class type, Method getter) {
        if (getter.getParameterTypes().length != 0) {
            fail(getter);
        }
        if (!getter.getReturnType().equals(type)) {
            fail(getter);
        }
        check(getter);
    }

    protected static void check(Method method) {
        int modifier;
        
        modifier = method.getModifiers();
        if (Modifier.isAbstract(modifier)) {
            fail(method);
        }
        if (Modifier.isStatic(modifier)) {
            fail(method);
        }
        if (!Modifier.isPublic(modifier)) {
            fail(method);
        }
    }
    
    protected static void fail(Method method) {
        throw new IllegalArgumentException(method.toString());
    }
    
    /* throws ItemException */ 
    protected static Object invoke(Method method, Object dest, Object ... args) {
        try {
            return method.invoke(dest, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new ItemException("cannot invoke " + method.getName(), e.getTargetException());
        }
    }

    //--
    
    /** name in singular. E.g. "package", not "packages" */
    private final String name;
    private final Cardinality cardinality;
    private final Type type;
    private final AnnotatedElement definition;
    
    public Item(String name, Cardinality cardinality, Type type, AnnotatedElement definition) {
        this.name = name;
        this.cardinality = cardinality;
        this.type = type;
        this.definition = definition;
    }
    
    public String getName() {
        return name;
    }

    public String getXmlName() {
        return xmlName(name);
    }
    
    public Cardinality getCardinality() {
        return cardinality;
    }

    public Type getType() {
        return type;
    }
    
    public AnnotatedElement getDefinition() {
        return definition;
    }

    public abstract Collection<T> get(Object src);
    public abstract void set(Object dest, Collection<T> values);

    public Collection<Instance<T>> getData(Object src) {
        Collection<T> objects;
        ArrayList<Instance<T>> result;
        
        objects = get(src);
        result = new ArrayList<Instance<T>>(objects.size());
        for (Object obj : objects) {
            result.add(new Instance<T>(type, (T) obj));
        }
        return result;
    }
    
    public T getOne(Object src) {
        Collection<T> all;
        
        all = get(src);
        if (all.size() != 1) {
            throw new IllegalStateException(all.toString());
        }
        return all.iterator().next();
    }
    
    public void setOne(Object dest, T value) {
        List<T> lst;
        
        lst = new ArrayList<T>();
        lst.add(value);
        set(dest, lst);
    }

    //--
    
    public static String xmlName(String name) {
        StringBuilder builder;
        char c;
        
        builder = new StringBuilder();
        for (int i = 0, max = name.length(); i < max; i++) {
            c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                builder.append('-');
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
