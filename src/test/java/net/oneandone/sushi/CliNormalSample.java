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
import net.oneandone.sushi.cli.Console;
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
        Cli cli;

        cli = Cli.create(World.create(), "demo help")
                .command(FirstCommand.class,    "first -flag=false -number=7 first remaining*")
                .command(SecondCommand.class,   "second");
        System.exit(cli.run("-v", "first", "one", "two", "three"));
    }

    public static class FirstCommand {
        private final Console console;

        private final String one;
        private final boolean flag;
        private final int number;
        private final List<String> remaining;

        public FirstCommand(Console console, boolean flag, int number, String one, List<String> remaining) {
            this.console = console;
            this.one = one;
            this.flag = flag;
            this.number = number;
            this.remaining = remaining;
        }

        public void run() {
            console.info.println("invoked 'first' with ");
            console.info.println("   flag = " + flag);
            console.info.println("   number = " + number);
            console.info.println("   one = " + one);
            console.info.println("   remaining = " + remaining);
        }
    }

    public static class SecondCommand {
        private final Console console;

        public SecondCommand(Console console) {
            this.console = console;
        }

        public void run() {
            console.verbose.println("verbose output");
            console.info.println("invoked 'second'");
        }
    }
}
