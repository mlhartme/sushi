package net.oneandone.sushi.cli;

public class Help {
    private final Console console;
    private final String help;

    public Help(Console console, String help) {
        this.console = console;
        this.help = help;
    }

    @Command("help")
    public void invoke() {
        console.info.println(help);
    }
}
