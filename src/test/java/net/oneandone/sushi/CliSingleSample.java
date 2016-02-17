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
import net.oneandone.sushi.cli.ExceptionHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Cli example with a single command.
  * <tt>
  * mvn org.codehaus.mojo:exec-maven-plugin:1.2.1:java -Dexec.classpathScope=test -Dexec.mainClass=net.oneandone.sushi.CliSample -Dexec.args=&quot;-flag -number 8 first second third&quot;
  * </tt>
*/
public class CliSingleSample {
    public static void main(String[] args) throws IOException {
        System.exit(new Cli()
                .context((ExceptionHandler) throwable -> {
                    throwable.printStackTrace();
                    return -1;
                })
                .command(CliSingleSample.class, "default -flag -number first remaining")
                .run(args));
    }

    private boolean flag = false;

    private int number = 7;

    private String first = null;

    private List<String> remaining = new ArrayList<>();

    public CliSingleSample() {
    }

    public void addRemaining(String str) {
        remaining.add(str);
    }

    public void run() {
        System.out.println("command invoked with ");
        System.out.println("   flag = " + flag);
        System.out.println("   number = " + number);
        System.out.println("   first = " + first);
        System.out.println("   remaining = " + remaining);
    }
}
