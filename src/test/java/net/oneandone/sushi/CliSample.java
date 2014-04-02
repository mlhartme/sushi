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
import net.oneandone.sushi.cli.Option;
import net.oneandone.sushi.cli.Remaining;
import net.oneandone.sushi.cli.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Usage sample.
  * <tt>
  * mvn org.codehaus.mojo:exec-maven-plugin:1.2.1:java -Dexec.classpathScope=test -Dexec.mainClass=net.oneandone.sushi.CliSample -Dexec.args=&quot;-flag -number 8 first second third&quot;
  * </tt>
*/
public class CliSample extends Cli implements Command {
    public static void main(String[] args) {
        System.exit(new CliSample().run(args));
    }

    @Option("flag")
    private boolean flag = false;
    
    @Option("number")
    private int number = 7;
    
    @Value(name = "first", position = 1)
    private String first = null;

    private List<String> remaining = new ArrayList<String>();
    
    @Remaining
    public void addRemaining(String str) {
        remaining.add(str);
    }
    
    @Override
    public void invoke() {
        console.info.println("command invoked with ");
        console.info.println("   flag = " + flag);
        console.info.println("   number = " + number);
        console.info.println("   first = " + first);
        console.info.println("   remaining = " + remaining);
    }
    
    @Override
    public void printHelp() {
        console.info.println("usage: [-flag | -number n] first remaining*");
    }
}
