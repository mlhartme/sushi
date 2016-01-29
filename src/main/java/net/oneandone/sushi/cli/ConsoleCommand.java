package net.oneandone.sushi.cli;

public abstract class ConsoleCommand {
    protected final Console console;

    public ConsoleCommand(Console console) {
        this.console = console;
    }

    @Option("v")
    public void setVerbose(boolean verbose) {
        console.setVerbose(verbose);
    }
}