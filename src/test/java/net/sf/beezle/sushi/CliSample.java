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
package net.sf.beezle.sushi;

import net.sf.beezle.sushi.cli.Cli;
import net.sf.beezle.sushi.cli.Command;
import net.sf.beezle.sushi.cli.Option;
import net.sf.beezle.sushi.cli.Remaining;
import net.sf.beezle.sushi.cli.Value;

import java.util.ArrayList;
import java.util.List;

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
