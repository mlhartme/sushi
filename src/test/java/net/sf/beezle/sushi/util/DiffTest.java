/*
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

package net.sf.beezle.sushi.util;

import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.World;
import net.sf.beezle.sushi.fs.file.FileNode;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DiffTest {
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
            right = dir.join(Strings.removeEnd(left.getName(), ".left") + ".right");
            try {
                expected = new Program((FileNode) world.getHome(), "diff", "-u", ((FileNode) left).getAbsolute(), right.getAbsolute()).exec();
            } catch (ExitCode e) {
                expected = e.output;
            }
            lines = Strings.split("\n", expected);
            lines.remove(0);
            lines.remove(0);
            expected = Strings.join("\n", lines);
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
        assertEquals(Arrays.asList(lines), Strings.lines(str));
    }
}
