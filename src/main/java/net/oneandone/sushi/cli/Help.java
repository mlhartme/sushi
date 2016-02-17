package net.oneandone.sushi.cli;

public class Help {
    private final Console console;
    private final String help;

    public Help(Console console, String help) {
        this.console = console;
        this.help = help;
    }

    public void run() {
        console.info.println(help);
    }
}
