package net.oneandone.sushi.cli;

import java.util.Locale;

/**
 * Created by mhm on 30.01.16.
 */
public class PackageVersion {
    private final Console console;

    public PackageVersion(Console console) {
        this.console = console;
    }

    @Command("version")
    public void invoke() {
        Package pkg;

        pkg = getClass().getPackage();
        if (pkg == null) {
            console.info.println("unknown version");
        } else {
            console.info.println(pkg.getName());
            console.info.println("  specification title: " + pkg.getSpecificationTitle());
            console.info.println("  specification version: " + pkg.getSpecificationVersion());
            console.info.println("  specification vendor: " + pkg.getSpecificationVendor());
            console.info.println("  implementation title: " + pkg.getImplementationTitle());
            console.info.println("  implementation version: " + pkg.getImplementationVersion());
            console.info.println("  implementation vendor: " + pkg.getImplementationVendor());
        }
        console.verbose.println("Platform encoding: " + System.getProperty("file.encoding"));
        console.verbose.println("Default Locale: " + Locale.getDefault());
        console.verbose.println("Scanner Locale: " + console.input.locale());
    }
}
