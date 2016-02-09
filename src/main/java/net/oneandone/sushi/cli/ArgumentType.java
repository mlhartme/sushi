package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;

import java.lang.reflect.ParameterizedType;
import java.util.List;

public class ArgumentType {
    protected static ArgumentType forReflect(Schema schema, java.lang.reflect.Type type) {
        ParameterizedType p;
        java.lang.reflect.Type[] args;

        if (type instanceof Class) {
            return new ArgumentType(false, schema.simple((Class) type));
        } else if (type instanceof ParameterizedType) {
            p = (ParameterizedType) type;
            args = p.getActualTypeArguments();
            if (!p.getRawType().equals(List.class)) {
                throw new IllegalArgumentException("not a list: " + type.toString());
            }
            if (args.length != 1) {
                throw new IllegalArgumentException("too many type parameter: " + type.toString());
            }
            if (!(args[0] instanceof Class)) {
                throw new IllegalArgumentException("too much nesting: " + type.toString());
            }
            return new ArgumentType(true, schema.simple((Class) args[0]));
        } else {
            throw new IllegalArgumentException("unsupported type: " + type);
        }
    }


    private final boolean list;
    private final SimpleType component;

    public ArgumentType(boolean list, SimpleType component) {
        this.list = list;
        this.component = component;
    }

    public boolean isList() {
        return list;
    }

    public boolean isBoolean() {
        return !list && component.getRawType().equals(Boolean.class);
    }

    public Object stringToComponent(String str) throws SimpleTypeException {
        return component.stringToValue(str);
    }

    public Object newComponent() {
        return component.newInstance();
    }
}
