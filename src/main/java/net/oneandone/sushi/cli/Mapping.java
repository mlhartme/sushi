package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.util.Separator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Mapping {
    public static Mapping parse(String str, Class<?> clazz) {
        Mapping result;
        int idx;

        result = new Mapping();
        for (String item : Separator.SPACE.split(str)) {
            idx = item.indexOf('=');
            if (idx != -1) {
                result.addField(item.substring(idx + 1), clazz, item.substring(0, idx));
            } else {
                idx = item.indexOf('(');
                if (idx != -1) {
                    if (!item.endsWith(")")) {
                        throw new IllegalArgumentException("invalid method mapping: " + item);
                    }
                    result.addMethod(item.substring(idx + 1, item.length() - 1), clazz, item.substring(0, idx));
                } else {
                    result.command(item);
                }
            }
        }
        return result;
    }

    private String command;

    /** maps argument names to field names */
    private final Map<String, Field> fields;

    /** maps argument names to method names */
    private final Map<String, Method> methods;

    public Mapping() {
        this.command = null;
        this.fields = new HashMap<>();
        this.methods = new HashMap<>();
    }

    public String getCommand() {
        return command;
    }

    public void command(String cmd) {
        if (command != null) {
            throw new IllegalArgumentException("duplicate command mapping");
        }
        command = cmd;
    }

    public void addField(String argument, Class<?> clazz, String name) {
        Field field;

        try {
            field = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("no such field: " + name);
        }
        if (fields.put(argument, field) != null) {
            throw new IllegalStateException("duplicate field mapping for argument " + argument);
        }
    }

    public void addMethod(String argument, Class<?> clazz, String name) {
        Method method;

        method = null;
        for (Method candidate : clazz.getMethods()) {
            if (candidate.getParameterCount() == 1 && candidate.getName().equals(name)) {
                if (method != null) {
                    throw new IllegalArgumentException("method mapping ambiguous: " + name);
                }
                method = candidate;
            }
        }
        if (method == null) {
            throw new IllegalArgumentException("method not found: " + clazz.getName() + "." + name + "(x)");
        }
        if (methods.put(argument, method) != null) {
            throw new IllegalArgumentException("duplicate method mapping for argument " + argument);
        }
    }

    public boolean contains(String name) {
        return fields.containsKey(name) || methods.containsKey(name);
    }

    public Target target(Schema schema, Object context, String argument) {
        Field field;
        Method method;

        field = fields.get(argument);
        if (field != null) {
            return TargetField.create(schema, field);
        }
        method = methods.get(argument);
        if (method != null) {
            return TargetMethod.create(schema, context, method);
        }
        throw new IllegalStateException();
    }
}
