package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.SimpleType;

public class ArgumentParameter extends Argument {
    private final Object[] actuals;
    private final int idx;

    protected ArgumentParameter(int position, String name, SimpleType type, int min, int max,
                                Object[] actuals, int idx, String dflt) {
        super(position, name, type, min, max, dflt);
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
