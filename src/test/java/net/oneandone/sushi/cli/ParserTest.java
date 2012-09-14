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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ParserTest {
    private static final World WORLD = new World();
    private static final Schema METADATA = new ReflectSchema(WORLD);

    @Test
    public void empty() {
        Parser parser;
        Empty empty;

        parser = Parser.create(METADATA, Empty.class);
        empty = new Empty();
        parser.run(empty);
        try {
            parser.run(empty, "-a", "10");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("unknown option"));
        }
        try {
            parser.run(empty, "a");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("unknown value"));
        }
        try {
            parser.run(empty, "-");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("unknown value"));
        }
    }

    @Test
    public void values() throws Exception {
        Parser parser;
        Values values;

        parser = Parser.create(METADATA, Values.class);

        values = new Values();
        try {
            parser.run(values);
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("missing"));
        }

        values = new Values();
        try {
            parser.run(values, "first");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("missing"));
        }

        values = new Values();
        parser.run(values, "first", "second");
        assertEquals("first", values.first.getName());
        assertEquals("second", values.second);
        assertEquals(0, values.remaining.size());

        values = new Values();
        parser.run(values, "first", "second", "third", "forth");
        assertEquals("first", values.first.getName());
        assertEquals("second", values.second);
        assertEquals(2, values.remaining.size());
        assertEquals(WORLD.file("third"), values.remaining.get(0));
        assertEquals(WORLD.file("forth"), values.remaining.get(1));

        values = new Values();
        parser.run(values, "first", "second", "-");
        assertEquals("first", values.first.getName());
        assertEquals("second", values.second);
        assertEquals(1, values.remaining.size());
        assertEquals(WORLD.file("-"), values.remaining.get(0));
    }

    @Test
    public void options() {
        Parser parser;
        Options options;

        parser = Parser.create(METADATA, Options.class);
        options = new Options();
        parser.run(options);
        assertEquals(0, options.first);
        assertNull(options.second);
        assertFalse(options.third);

        options = new Options();
        assertSame(options, parser.run(options, "-first", "1"));
        assertEquals(1, options.first);
        assertNull(options.second);
        assertFalse(options.third);

        options = new Options();
        try {
            assertSame(options, parser.run(options, "-first"));
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("missing value"));
        }

        options = new Options();
        parser.run(options, "-second", "foo");
        assertEquals(0, options.first);
        assertEquals("foo", options.second);
        assertFalse(options.third);

        options = new Options();
        parser.run(options, "-third");
        assertEquals(0, options.first);
        assertNull(options.second);
        assertTrue(options.third);

        options = new Options();
        parser.run(options, "-third", "-first", "-1", "-second", "bar");
        assertEquals(-1, options.first);
        assertEquals("bar", options.second);
        assertTrue(options.third);

        options = new Options();
        try {
            parser.run(options, "-first");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage().contains("missing"));
        }

        options = new Options();
        try {
            parser.run(options, "-first", "a");
            fail();
        } catch (ArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("expected integer"));
        }
    }

    @Test
    public void children() {
        Parser parser;
        Children children;
        ChildObject child;

        children = new Children();
        parser = Parser.create(METADATA, Children.class);
        child = (ChildObject) parser.run(children, "a");
        assertEquals("a", child.name);
        assertEquals(0, child.remaining.size());

        children = new Children();
        parser = Parser.create(METADATA, Children.class);
        child = (ChildObject) parser.run(children, "b", "1", "2");
        assertEquals("b", child.name);
        assertEquals(Arrays.asList("1", "2"), child.remaining);
    }

    @Test
    public void childAndValue() {
        Parser parser;
        ChildAndValue instance;
        ChildObject child;

        parser = Parser.create(METADATA, ChildAndValue.class);
        instance = new ChildAndValue();
        child = (ChildObject) parser.run(instance, "a");
        assertEquals("a", child.name);
        assertNull(instance.b);

        instance = new ChildAndValue();
        assertSame(instance, parser.run(instance, "b"));
        assertEquals("b", instance.b);
   }

    //-- various defining classes

    public static class Empty {
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
    }

    public static class Values {
        public FileNode first;
        @Value(name = "second", position = 2)
        public String second;
        public List<FileNode> remaining = new ArrayList<FileNode>();

        @Value(name = "first", position = 1)
        public void first(FileNode first) {
            this.first = first;
        }

        @Remaining(name = "remaining")
        public void remaining(FileNode str) {
            remaining.add(str);
        }
    }

    public static class Children {
        @Child("a")
        public Object a() {
            return new ChildObject("a");
        }

        @Child("b")
        public Object b() {
            return new ChildObject("b");
        }
    }

    public static class ChildAndValue {
        @Child("a")
        public Object a() {
            return new ChildObject("a");
        }

        @Value(name = "b", position = 1)
        public String b;
    }

    public static class ChildObject {
        public final String name;
        public final List<String> remaining;

        public ChildObject(String name) {
            this.name = name;
            this.remaining = new ArrayList<String>();
        }

        @Remaining(name = "remaining")
        public void remaining(String str) {
            remaining.add(str);
        }

        public void invoke() {
        }
    }
}
