package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.SimpleType;

public class ArgumentParameter extends Argument {
    private final Object[] actuals;
    private final int idx;

    protected ArgumentParameter(ArgumentDeclaration declaration, SimpleType type, Object[] actuals, int idx) {
        super(declaration, type);
        this.actuals = actuals;
        this.idx = idx;
    }

    @Override
    public boolean before() {
        return true;
    }

    @Override
    public void set(Object obj, Object value) {
        actuals[idx] = value;
    }
}
