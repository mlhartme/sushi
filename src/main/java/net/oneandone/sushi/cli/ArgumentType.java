package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.SimpleType;
import net.oneandone.sushi.metadata.SimpleTypeException;

public class ArgumentType {
    protected static ArgumentType forReflect(Schema schema, java.lang.reflect.Type type) {
        if (type instanceof Class) {
            return new ArgumentType(schema.simple((Class) type));
        } else {
            throw new IllegalArgumentException(type.toString());
        }
    }


    private final SimpleType component;

    public ArgumentType(SimpleType component) {
        this.component = component;
    }

    public boolean isBoolean() {
        return component.getRawType().equals(Boolean.class);
    }

    public Object stringToValue(String str) throws SimpleTypeException {
        return component.stringToValue(str);
    }

    public Object newInstance() {
        return component.newInstance();
    }
}
