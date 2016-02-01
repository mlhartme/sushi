package net.oneandone.sushi.cli;

/**
 * Created by mhm on 01.02.16.
 */
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
