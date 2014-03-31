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
package net.oneandone.sushi.metadata;

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

    public Item(String name, Cardinality cardinality, Type type) {
        this.name = name;
        this.cardinality = cardinality;
        this.type = type;
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
    
    public abstract Collection<T> get(Object src);
    public abstract void set(Object dest, Collection<T> values);

    public Collection<Instance<T>> getInstances(Object src) {
        Collection<T> objects;
        ArrayList<Instance<T>> result;
        
        objects = get(src);
        result = new ArrayList<>(objects.size());
        for (Object obj : objects) {
            result.add(new Instance<>(type, (T) obj));
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
