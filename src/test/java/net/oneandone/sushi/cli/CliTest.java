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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CliTest {
    private static final World WORLD = World.createMinimal();

    private static ContextBuilder parser(Class<?> clazz, String syntax) {
        Cli cli;

        cli = Cli.create(WORLD, "").command(clazz, "foo " + syntax);
        return cli.command("foo").getBuilder();
    }

    @Test
    public void empty() throws Throwable {
        ContextBuilder parser;

        parser = parser(Empty.class, "");
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
        ContextBuilder parser;
        Values values;

        parser = parser(Values.class, "first second third remaining* { second=second third(third) remaining*(remaining) }");
        try {
            parser.run();
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("missing"));
        }

        try {
            parser.run("1");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("missing"));
        }

        values = (Values) parser.run("1", "second", "third");
        assertEquals(1, values.first);
        assertEquals("second", values.second);
        assertEquals("third", values.third.getName());
        assertEquals(0, values.remaining.size());

        values = (Values) parser.run("2", "second", "third", "forth", "fifth");
        assertEquals(2, values.first);
        assertEquals("second", values.second);
        assertEquals("third", values.third.getName());
        assertEquals(2, values.remaining.size());
        assertEquals(WORLD.file("forth"), values.remaining.get(0));
        assertEquals(WORLD.file("fifth"), values.remaining.get(1));
    }

    @Test
    public void options() throws Throwable {
        ContextBuilder parser;
        Options options;

        parser = parser(Options.class, "-first -second=bla -third { first=first node(second) }");
        options = (Options) parser.run();
        assertEquals(0, options.first);
        assertEquals("bla", options.second);
        assertFalse(options.third);

        options = (Options) parser.run("-first", "1");
        assertEquals(1, options.first);
        assertEquals("bla", options.second);
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
        assertEquals("bla", options.second);
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
    public void contextClass() {
        Cli cli;

        lastWithContext = null;
        cli = Cli.create(WORLD, "no help text");
        cli.context(Values.class, "first");
        cli.command(WithContext.class, "cmd l");
        cli.run("cmd", "42", "2");
        assertTrue(lastWithContext instanceof WithContext);
        assertEquals(42, lastWithContext.values.first);
        assertEquals(2, lastWithContext.l);
    }

    //-- various command classes

    public static class Empty {
        public void run() {}
    }

    public static class Values {
        public int first;
        public String second;
        public FileNode third;
        public List<FileNode> remaining = new ArrayList<>();

        public Values(int first) {
            this.first = first;
        }

        public void third(FileNode third) {
            this.third = third;
        }

        public void remaining(FileNode str) {
            remaining.add(str);
        }

        public void run() {
        }
    }

    public static class Options {
        private int first;

        public String second;

        protected final boolean third;

        public Options(boolean third) {
            this.third = third;
        }

        public void node(String second) {
            this.second = second;
        }

        public void run() {
        }
    }

    private static WithContext lastWithContext;

    public static class WithContext {
        public final Values values;
        public final long l;

        public WithContext(Values values, long l) {
            this.values = values;
            this.l = l;
        }

        public void run() {
            lastWithContext = this;
        }
    }
}
