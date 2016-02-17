package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class Ctx {
    public static Ctx create(Object object, String syntax, String mapping) {
        return new Ctx(object, Source.forSyntax(syntax), Mapping.parse(mapping, object.getClass()));
    }

    public final Object object;
    public final List<Source> sources;
    public final Mapping mapping;

    public Ctx(Object object, List<Source> sources, Mapping mapping) {
        this.object = object;
        this.sources = sources;
        this.mapping = mapping;
    }

    public CommandParser createParser(Schema schema, List<Ctx> contexts) {
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
        clazz = (Class<?>) object;
        for (Constructor constructor : clazz.getDeclaredConstructors()) {
            arguments.clear();
            actuals = match(schema, constructor, contexts, constructorSources, arguments);
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
        for (Source s : extraSources) {
            result.addArgument(new Argument(s, mapping.target(schema, null /* */, s.getName())));
        }
        return result;
    }

    private static Object[] match(Schema schema, Constructor constructor,
                                  List<Ctx> initialContexts, List<Source> initialSources, List<Argument> result) {
        List<Ctx> remainingContext;
        List<Source> remainingSources;
        Parameter[] formals;
        Object[] actuals;
        Parameter formal;
        Object ctx;
        Source source;

        remainingContext = new ArrayList<>(initialContexts);
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

    private static Object eatContext(List<Ctx> contexts, Class<?> type) {
        Object obj;

        for (int i = 0, max = contexts.size(); i < max; i++) {
            obj = contexts.get(i).object;
            if (type.isAssignableFrom(obj.getClass())) {
                contexts.remove(i);
                return obj;
            }
        }
        return null;
    }

}
