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
package net.oneandone.sushi.util;

import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.World;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.launcher.ExitCode;
import net.oneandone.sushi.launcher.Launcher;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DiffTest {
    public static final void main(String[] args) throws IOException {
        World world;
        String smallLeft;
        String smallRight;
        String smallDiff;
        long started;
        long ms;

        world = new World();
        smallLeft = world.getHome().join("left.txt").readString();
        smallRight = world.getHome().join("right.txt").readString();
        started = System.currentTimeMillis();
        smallDiff = Diff.diff(smallLeft, smallRight);
        ms = System.currentTimeMillis() - started;
        System.out.println(smallDiff);
        System.out.println("ms=" + ms);
    }

    @Test
    public void empty() {
        assertEquals("", Diff.diff("", ""));
        assertEquals("", Diff.diff("\n\n", "\n\n"));
    }

    @Test
    public void addall() {
        assertEquals("+abc", Diff.diff("", "abc"));
        assertEquals("+abc\n", Diff.diff("", "abc\n"));
        assertEquals("+abc\n+xyz", Diff.diff("", "abc\nxyz"));
        assertEquals("+\n+\n+\n", Diff.diff("", "\n\n\n"));
    }

    @Test
    public void addone() {
        assertEquals("+b\n", Diff.diff("a\nc\n", "a\nb\nc\n"));
    }

    @Test
    public void removeall() {
        assertEquals("-abc", Diff.diff("abc", ""));
        assertEquals("-abc\n", Diff.diff("abc\n", ""));
        assertEquals("-abc\n-xyz", Diff.diff("abc\nxyz", ""));
        assertEquals("-\n-\n-\n", Diff.diff("\n\n\n", ""));
    }

    @Test
    public void removeone() {
        assertEquals("-b\n", Diff.diff("a\nb\nc\n", "a\nc\n"));
    }

    @Test
    public void changeall() {
        assertEquals("-a\n-b\n+1\n+2\n", Diff.diff("0\na\nb\nc\n", "0\n1\n2\nc\n"));
    }

    @Test
    public void mixed() {
        assertEquals("-b\n-B\n+d\n+D\n", Diff.diff("a\nb\nB\nc\n", "a\nc\nd\nD\n"));
    }

    @Test
    public void replace() {
        assertEquals("-before\n+after\n", Diff.diff("before\nsame\n", "after\nsame\n"));
    }

    @Test
    public void context() {
        assertEquals(" 3\n-before\n+after\n X\n Y\n-in\n 4\n", Diff.diff("1\n2\n3\nbefore\nX\nY\nin\n4\n5\n6\n7\n", "1\n2\n3\nafter\nX\nY\n4\n5\n6\n7\n", false, 1));
        assertEquals(" 2\n 3\n-before\n+after\n X\n Y\n-in\n 4\n 5\n", Diff.diff("1\n2\n3\nbefore\nX\nY\nin\n4\n5\n6\n7\n", "1\n2\n3\nafter\nX\nY\n4\n5\n6\n7\n", false, 2));
        assertEquals(" 1\n 2\n 3\n-before\n+after\n X\n Y\n-in\n 4\n 5\n 6\n", Diff.diff("1\n2\n3\nbefore\nX\nY\nin\n4\n5\n6\n7\n", "1\n2\n3\nafter\nX\nY\n4\n5\n6\n7\n", false, 3));
        assertEquals(" 1\n 2\n 3\n-before\n+after\n X\n Y\n-in\n 4\n 5\n 6\n 7\n", Diff.diff("1\n2\n3\nbefore\nX\nY\nin\n4\n5\n6\n7\n", "1\n2\n3\nafter\nX\nY\n4\n5\n6\n7\n", false, 4));
    }

    @Test
    public void files() throws IOException {
        World world;
        FileNode dir;
        FileNode right;
        String expected;
        String actual;
        List<String> lines;

        world = new World();
        dir = world.guessProjectHome(getClass()).join("src/test/resources/diff");
        for (Node left : dir.find("*.left")) {
            right = dir.join(Strings.removeRight(left.getName(), ".left") + ".right");
            try {
                expected = new Launcher((FileNode) world.getHome(), "diff", "-u", ((FileNode) left).getAbsolute(), right.getAbsolute()).exec();
            } catch (ExitCode e) {
                expected = e.output;
            }
            lines = Separator.on('\n').split(expected);
            lines.remove(0);
            lines.remove(0);
            expected = Separator.on('\n').join(lines);
            actual = Diff.diff(left.readString(), right.readString(), true, 3);
            assertEquals(left.getPath(), expected, actual);
        }
    }

    //--

    @Test
    public void lines() {
        lines("");
        lines("\n", "\n");
        lines("abcde", "abcde");
        lines("a\n", "a\n");
        lines("a\nb", "a\n", "b");
        lines("a\nb\n", "a\n", "b\n");
    }

    private void lines(String str, String ... lines) {
        assertEquals(Arrays.asList(lines), Separator.RAW_LINE.split(str));
    }
}
