package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.SimpleType;

public class ArgumentParameter extends Argument {
    public final int position;
    private final Object[] actuals;
    private final int idx;

    protected ArgumentParameter(String name, SimpleType type, int min, int max,
                                int position, Object[] actuals, int idx) {
        super(name, type, min, max);
        this.position = position;
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
