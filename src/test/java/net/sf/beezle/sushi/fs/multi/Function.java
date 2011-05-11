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
