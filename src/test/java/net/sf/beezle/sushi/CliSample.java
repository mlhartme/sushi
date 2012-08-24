/**
 * Copyright 1&1 Internet AG, http://www.1and1.org
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
