/**
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.sushi.cli;

import java.util.Locale;

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
