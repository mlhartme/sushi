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
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.PackageVersion;
import net.oneandone.sushi.cli.Value;
import net.oneandone.sushi.fs.World;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cli example with a single command.
  * <tt>
  * mvn org.codehaus.mojo:exec-maven-plugin:1.2.1:java -Dexec.classpathScope=test -Dexec.mainClass=net.oneandone.sushi.CliSample -Dexec.args=&quot;-flag -number 8 first second third&quot;
  * </tt>
*/
public class CliNormalSample {
    public static void main(String[] args) throws IOException {
        World world;

        world = World.create();
        System.exit(new Cli(world)
                .addContext(Console.create(world))
                .addCommand(FirstCommand.class)
                .addCommand(SecondCommand.class)
                .addCommand(PackageVersion.class)
                .addHelp("demo help").addDefaultCommand("help")
                .run("first", "second"));
    }

    public static class FirstCommand {
        private final Console console;

        @Option("flag")
        private boolean flag = false;

        @Option("number")
        private int number = 7;

        private final String first;

        private final List<String> remaining = new ArrayList<>();

        public FirstCommand(@Context Console console, @Value("first") String first) {
            this.console = console;
            this.first = first;
        }

        @Value(value = "remaining", position = 2, min = 0, max = Integer.MAX_VALUE)
        public void addRemaining(String str) {
            remaining.add(str);
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
