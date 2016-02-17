package net.oneandone.sushi.cli;

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

}
