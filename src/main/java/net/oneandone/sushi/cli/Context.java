package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Context {
    public static Context create(Context parent, Object classOrInstance, String syntax) {
        int idx;
        String mapping;

        idx = syntax.indexOf('{');
        if (idx == -1) {
            mapping = "";
        } else {
            if (!syntax.endsWith("}")) {
                throw new IllegalArgumentException(syntax);
            }
            mapping = syntax.substring(idx + 1, syntax.length() - 1).trim();
            syntax = syntax.substring(0, idx).trim();
        }
        return new Context(parent, classOrInstance, Source.forSyntax(syntax), Mapping.parse(mapping, classOrInstance.getClass()));
    }

    /** may be null */
    public final Context parent;
    public final Object classOrInstance;
    public final List<Source> sources;
    public final Mapping mapping;

    public Context(Context parent, Object classOrInstance, List<Source> sources, Mapping mapping) {
        this.parent = parent;
        this.classOrInstance = classOrInstance;
        this.sources = sources;
        this.mapping = mapping;
    }

    public CommandParser createParser(Schema schema) {
        Class<?> clazz;
        List<Source> constructorSources;
        List<Source> extraSources;
        Object[] actuals;
        List<Argument> arguments;
        Constructor found;
        Object[] foundActuals;
        List<Argument> foundArguments;
        CommandParser result;

        found = null;
        foundActuals = null;
        foundArguments = null;
        arguments = new ArrayList<>();
        constructorSources = new ArrayList<>(sources.size());
        extraSources = new ArrayList<>();
        for (Source s : sources) {
            if (mapping.contains(s.getName())) {
                extraSources.add(s);
            } else {
                constructorSources.add(s);
            }
        }
        if (classOrInstance instanceof Class) {
            clazz = (Class<?>) classOrInstance;
            for (Constructor constructor : clazz.getDeclaredConstructors()) {
                arguments.clear();
                actuals = match(schema, constructor, constructorSources, arguments);
                if (actuals != null) {
                    if (found != null) {
                        throw new IllegalStateException("constructor is ambiguous");
                    }
                    found = constructor;
                    foundActuals = actuals;
                    foundArguments = new ArrayList<>(arguments);
                }
            }
            if (found == null) {
                throw new IllegalStateException(clazz + ": no matching constructor");
            }
            result = new CommandParser(found, foundActuals);
            for (Argument a : foundArguments) {
                result.addArgument(a);
            }
        } else {
            if (!constructorSources.isEmpty()) {
                throw new IllegalStateException("cannot apply constructor argument to a command instance");
            }
            result = new CommandParser(classOrInstance);
        }
        for (Source s : extraSources) {
            result.addArgument(new Argument(s, mapping.target(schema, null /* */, s.getName())));
        }
        if (parent != null) {
            parent.addContextCommands(schema, result);
        }
        return result;
    }

    private void addContextCommands(Schema schema, CommandParser parser) {
        for (Source s : sources) {
            if (mapping.contains(s.getName())) {
                if (classOrInstance instanceof Class) {
                    throw new IllegalStateException("context instance expected");
                }
                parser.addArgument(new Argument(s, mapping.target(schema, classOrInstance, s.getName())));
            } else {
                throw new IllegalStateException("invalid constructor argument for context object");
            }
        }
        if (parent != null) {
            parent.addContextCommands(schema, parser);
        }
    }

    private Object[] match(Schema schema, Constructor constructor, List<Source> initialSources, List<Argument> result) {
        List<Context> remainingContext;
        List<Source> remainingSources;
        Parameter[] formals;
        Object[] actuals;
        Parameter formal;
        Object ctx;
        Source source;

        remainingContext = parentList();
        remainingSources = new ArrayList<>(initialSources);
        formals = constructor.getParameters();
        actuals = new Object[formals.length];
        for (int i = 0; i < formals.length; i++) {
            formal = formals[i];
            ctx = eatContext(remainingContext, formal.getType());
            if (ctx != null) {
                actuals[i] = ctx;
            } else if (remainingSources.isEmpty()) {
                return null; // too many constructor arguments
            } else {
                source = remainingSources.remove(0);
                result.add(new Argument(source, new TargetParameter(schema, formal.getParameterizedType(), actuals, i)));
            }
        }
        if (!remainingSources.isEmpty()) {
            return null; // not all arguments matched
        }
        return actuals;
    }

    private static Object eatContext(List<Context> parents, Class<?> type) {
        Object obj;

        for (int i = 0, max = parents.size(); i < max; i++) {
            obj = parents.get(i).classOrInstance;
            if (type.isAssignableFrom(obj.getClass())) {
                parents.remove(i);
                return obj;
            }
        }
        return null;
    }

    public List<Context> parentList() {
        List<Context> result;

        result = new ArrayList<>();
        for (Context context = parent; context != null; context = context.parent) {
            result.add(parent);
        }
        return result;
    }
}
