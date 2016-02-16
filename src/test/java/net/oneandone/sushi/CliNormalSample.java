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
package net.oneandone.sushi;

import net.oneandone.sushi.cli.Cli;
import net.oneandone.sushi.cli.Command;
import net.oneandone.sushi.cli.Console;
import net.oneandone.sushi.cli.Context;
import net.oneandone.sushi.cli.Help;
import net.oneandone.sushi.cli.PackageVersion;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.List;

/**
 * Cli example with a single command.
  * <tt>
  * mvn org.codehaus.mojo:exec-maven-plugin:1.2.1:java -Dexec.classpathScope=test -Dexec.mainClass=net.oneandone.sushi.CliSample -Dexec.args=&quot;-flag -number 8 first second third&quot;
  * </tt>
*/
public class CliNormalSample {
    public static void main(String[] args) throws IOException {
        Console console;

        console = Console.create(World.create());
        System.exit(new Cli(console.world)
                .addContext(console)
                .addSyntaxCommand("first -flag=true -number=7 first remaining*", FirstCommand.class)
                .addCommand(SecondCommand.class)
                .addCommand(PackageVersion.class)
                .addCommandInstance(new Help(console, "demo help"))
                .addDefaultCommand("help")
                .run("first", "a", "b", "c"));
    }

    public static class FirstCommand {
        private final Console console;

        private final String first;
        private final boolean flag;
        private final int number;
        private final List<String> remaining;

        public FirstCommand(Console console, boolean flag, int number, String first, List<String> remaining) {
            this.console = console;
            this.first = first;
            this.flag = flag;
            this.number = number;
            this.remaining = remaining;
        }

        @Command("first")
        public void run() {
            console.info.println("invoked 'first' with ");
            console.info.println("   flag = " + flag);
            console.info.println("   number = " + number);
            console.info.println("   first = " + first);
            console.info.println("   remaining = " + remaining);
        }
    }

    public static class SecondCommand {
        private final Console console;

        public SecondCommand(@Context Console console) {
            this.console = console;
        }

        @Command("second")
        public void run() {
            console.info.println("invoked 'second'");
        }
    }
}
