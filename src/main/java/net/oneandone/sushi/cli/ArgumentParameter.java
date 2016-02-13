package net.oneandone.sushi.cli;

public class ArgumentParameter extends Argument {
    private final Object[] actuals;
    private final int idx;

    protected ArgumentParameter(Source source, Target type, Object[] actuals, int idx) {
        super(source, type);
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
