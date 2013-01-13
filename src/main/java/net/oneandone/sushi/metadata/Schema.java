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

import net.oneandone.sushi.metadata.simpletypes.BooleanType;
import net.oneandone.sushi.metadata.simpletypes.CharacterType;
import net.oneandone.sushi.metadata.simpletypes.ClassType;
import net.oneandone.sushi.metadata.simpletypes.DoubleType;
import net.oneandone.sushi.metadata.simpletypes.EnumType;
import net.oneandone.sushi.metadata.simpletypes.FloatType;
import net.oneandone.sushi.metadata.simpletypes.IntType;
import net.oneandone.sushi.metadata.simpletypes.LongType;
import net.oneandone.sushi.metadata.simpletypes.MethodType;
import net.oneandone.sushi.metadata.simpletypes.StringType;
import net.oneandone.sushi.metadata.simpletypes.VoidType;
import net.oneandone.sushi.util.Reflect;
import net.oneandone.sushi.util.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
 * A set of Types. Initially, the set consists of simple types only. Complex types
 * can be created explicitly by invoking the add method or implicitly by overriding the complex
 * method. Thus, metadata can be used as a factory for complex types.
 */
public class Schema {
    private final Map<Class<?>, Type> map;
    
    public Schema() {
        map = new HashMap<>();
        add(new StringType(this));
        add(new IntType(this));
        add(new LongType(this));
        add(new FloatType(this));
        add(new DoubleType(this));
        add(new BooleanType(this));
        add(new CharacterType(this));
        add(new VoidType(this));
        add(new ClassType(this));
        add(new MethodType(this));
    }
    
    public ComplexType complex(Class<?> clazz) {
        return (ComplexType) type(clazz);
    }

    public SimpleType simple(Class<?> clazz) {
        return (SimpleType) type(clazz);
    }

    /**
     * This method is the main purpose of this class.
     * @return never null 
     */
    public Type type(Class<?> clazz) {
        Type type;
        
        if (clazz.isPrimitive()) {
            clazz = Reflect.getWrapper(clazz);
        }
        type = map.get(clazz);
        if (type == null) {
            if (Enum.class.isAssignableFrom(clazz)) {
                type = EnumType.create(this, (Class) clazz);
            } else {
                type = new ComplexType(this, clazz, typeName(clazz));
            }
            map.put(clazz, type);
            if (Enum.class.isAssignableFrom(clazz)) {
                // nothing
            } else {
                complex((ComplexType) type);
            }
        }
        return type;
    }

    public Type type(String name) {
        for (Type type : map.values()) {
            if (name.equals(type.getName())) {
                return type;
            }
        }
        throw new IllegalArgumentException(name);
    }
    
    public <T> Instance<T> instance(T obj) {
        Type type;
        
        type = type(obj.getClass());
        return new Instance<T>(type, obj);
    }
    
    public <T> List<Instance<T>> instances(Collection<T> col) {
        List<Instance<T>> result;
        
        result = new ArrayList<>();
        for (T obj : col) {
            result.add(instance(obj));
        }
        return result;
    }

    public void add(Type type) {
        map.put(type.getType(), type);
    }
    
    public void complex(ComplexType type) {
        throw new UnsupportedOperationException(type.getName());
    }

    public static String typeName(Class<?> clazz) {
        String name;
        
        name = clazz.getName();
        name = name.substring(name.lastIndexOf(".") + 1); // ok for -1
        // simplify inner class names ... 
        name = name.substring(name.indexOf('$') + 1); // ok for -1
        return Strings.decapitalize(name);
    }
}
