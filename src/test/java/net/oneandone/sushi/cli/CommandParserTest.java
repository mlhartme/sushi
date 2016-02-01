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

import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.metadata.Schema;
import net.oneandone.sushi.metadata.reflect.ReflectSchema;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CommandParserTest {
    private static final World WORLD = World.createMinimal();
    private static final Schema METADATA = new ReflectSchema(WORLD);

    @Test
    public void empty() throws Throwable {
        CommandParser parser;

        parser = CommandParser.create(METADATA, Empty.class);
        assertTrue(parser.run() instanceof Empty);
        try {
            parser.run("-a", "10");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("unknown option"));
        }
        try {
            parser.run("a");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("unknown value"));
        }
        try {
            parser.run("-");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("unknown value"));
        }
    }

    @Test
    public void values() throws Throwable {
        CommandParser parser;
        Values values;

        parser = CommandParser.create(METADATA, Values.class);
        try {
            parser.run();
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("missing"));
        }

        try {
            parser.run("first");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("missing"));
        }

        values = (Values) parser.run("first", "second");
        assertEquals("first", values.first.getName());
        assertEquals("second", values.second);
        assertEquals(0, values.remaining.size());

        values = (Values) parser.run("first", "second", "third", "forth");
        assertEquals("first", values.first.getName());
        assertEquals("second", values.second);
        assertEquals(2, values.remaining.size());
        assertEquals(WORLD.file("third"), values.remaining.get(0));
        assertEquals(WORLD.file("forth"), values.remaining.get(1));

        values = (Values) parser.run("first", "second", "-");
        assertEquals("first", values.first.getName());
        assertEquals("second", values.second);
        assertEquals(1, values.remaining.size());
        assertEquals(WORLD.file("-"), values.remaining.get(0));
    }

    @Test
    public void options() throws Throwable {
        CommandParser parser;
        Options options;

        parser = CommandParser.create(METADATA, Options.class);
        options = (Options) parser.run();
        assertEquals(0, options.first);
        assertNull(options.second);
        assertFalse(options.third);

        options = (Options) parser.run("-first", "1");
        assertEquals(1, options.first);
        assertNull(options.second);
        assertFalse(options.third);

        try {
            parser.run("-first");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("missing value"));
        }

        options = (Options) parser.run("-second", "foo");
        assertEquals(0, options.first);
        assertEquals("foo", options.second);
        assertFalse(options.third);

        options = (Options) parser.run("-third");
        assertEquals(0, options.first);
        assertNull(options.second);
        assertTrue(options.third);

        options = (Options) parser.run("-third", "-first", "-1", "-second", "bar");
        assertEquals(-1, options.first);
        assertEquals("bar", options.second);
        assertTrue(options.third);

        try {
            parser.run("-first");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("missing"));
        }

        try {
            parser.run("-first", "a");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("expected integer"));
        }
    }

    @Test
    public void constr() throws Throwable {
        CommandParser parser;
        Constr constr;

        parser = CommandParser.create(METADATA, Constr.class);
        constr = (Constr) parser.run("a", "-o", "1");
        assertEquals("a", constr.v);
        assertEquals(1, constr.o);
    }
    
    @Test
    public void normal() throws Throwable {
        Cli cli;
        Options options;

        cli = new Cli().addCommand(Empty.class, Options.class, Values.class);
        assertTrue(cli.parseNormal("empty")[1] instanceof Empty);
        options = (Options) cli.parseNormal("options", "-first", "32")[1];
        assertEquals(32, options.first);
        try {
            cli.parseNormal("notfound");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("command not found"));
        }
    }

    @Test
    public void single() throws Throwable {
        Cli cli;
        Values values;

        cli = new Cli().addCommand(Values.class);
        values = (Values) cli.parseSingle(Arrays.asList("first", "second", "32"))[1];
        assertEquals("first", values.first.getName());
        assertEquals("second", values.second);
        assertEquals(1, values.remaining.size());
    }

    //-- various defining classes

    public static class Empty {
        @Command("empty")
        public void cmd() {}
    }

    public static class Options {
        @Option("first")
        private int first;

        public String second;

        @Option("second")
        public void node(String second) {
            this.second = second;
        }

        @Option("third")
        protected boolean third;

        @Command("options")
        public void cmd() {}
    }

    public static class Values {
        public FileNode first;
        @Value(name = "second", position = 2)
        public String second;
        public List<FileNode> remaining = new ArrayList<>();

        @Value(name = "first", position = 1)
        public void first(FileNode first) {
            this.first = first;
        }

        @Value(name = "remaining", position = 3, min = 0, max = Integer.MAX_VALUE)
        public void remaining(FileNode str) {
            remaining.add(str);
        }

        @Command("values")
        public void run() {
        }
    }

    public static class Constr {
        public final String v;
        public final int o;

        public Constr(@Value(name = "v", position = 1, min = 1, max = 1) String v,
                      @Option("o") int o) {
            this.v = v;
            this.o = o;
        }

        @Command("constr")
        public void run() {
        }
    }
}
