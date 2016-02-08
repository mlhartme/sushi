package net.oneandone.sushi.cli;

public class ArgumentParameter extends Argument {
    private final Object[] actuals;
    private final int idx;

    protected ArgumentParameter(ArgumentDeclaration declaration, Object[] actuals, int idx) {
        super(declaration);
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
