package net.oneandone.sushi.cli;

import net.oneandone.sushi.metadata.Schema;

public class ArgumentParameter extends Target {
    private final Object[] actuals;
    private final int idx;

    protected ArgumentParameter(Schema schema, java.lang.reflect.Type type, Object[] actuals, int idx) {
        super(schema, type);
        this.actuals = actuals;
        this.idx = idx;
    }

    @Override
    public boolean before() {
        return true;
    }

    @Override
    public void doSet(Object obj, Object value) {
        actuals[idx] = value;
    }
}
