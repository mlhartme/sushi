package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;

import java.lang.reflect.ParameterizedType;
import java.util.List;

/** Defines where to store values from the command line. */
public abstract class Target {
    private final boolean list;
    private final SimpleType component;

    public Target(Schema schema, java.lang.reflect.Type type) {
        ParameterizedType p;
        java.lang.reflect.Type[] args;

        if (type instanceof Class) {
            this.list = false;
            this.component = schema.simple((Class) type);
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
            this.list = true;
            this.component = schema.simple((Class) args[0]);
        } else {
            throw new IllegalArgumentException("unsupported type: " + type);
        }
    }

    public Target(boolean list, SimpleType component) {
        this.list = list;
        this.component = component;
    }

    /** return true to set pass values before command object instantiation */
    public abstract boolean before();

    /** value is an instance of the reflection type */
    public abstract void doSet(Object dest, Object value);

    public boolean isList() {
        return list;
    }

    public boolean isBoolean() {
        return !list && (component.getRawType().equals(Boolean.TYPE) || component.getRawType().equals(Boolean.class));
    }

    public Object stringToComponent(String str) throws SimpleTypeException {
        return component.stringToValue(str);
    }

    public Object newComponent() {
        return component.newInstance();
    }
}
