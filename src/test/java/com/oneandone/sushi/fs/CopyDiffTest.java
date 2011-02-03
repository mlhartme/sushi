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

package com.oneandone.sushi.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import com.oneandone.sushi.util.Substitution;

public class CopyDiffTest {
	private IO io;
	private Map<String, String> variables;
	private Copy copy;
	
	public CopyDiffTest() throws IOException {
		io = new IO();
		variables = new HashMap<String, String>();
        copy = new Copy(io.getTemp().createTempDirectory(),
                io.filter().includeAll(), true, 
                variables, Substitution.path(), Substitution.ant(), Copy.DEFAULT_CONTEXT_DELIMITER, Copy.DEFAULT_CALL_PREFIX);
	}

    @Test
    public void mode() throws Exception {
        Node destdir;
        Node file;
        
        destdir = copy.getSourceDir().getIO().getTemp().createTempDirectory();

        file = copy.getSourceDir().join("file");
        file.writeString("foo");
        file.setMode(0700);
        copy.directory(destdir);
        assertEquals(0700, destdir.join("file").getMode());

        file.setMode(0655);
        assertEquals("m file\n", brief(destdir));
        copy.directory(destdir);
        assertEquals(0655, destdir.join("file").getMode());
    }
    
	@Test
	public void diff() throws Exception {
		Node left;
		Node right;
		
		left = io.getTemp().createTempDirectory();
		right = io.getTemp().createTempDirectory();
		left.join("left").writeString("1");
		right.join("right").writeString("2");
		assertEquals("R left\nA right\n", left.diffDirectory(right, true));
		assertEquals("A right\n", new Diff(true).directory(left, right, "right"));
	}

	@Test
    public void template() throws Exception {
        Node destdir;
        String brief;
        String normal;
        
        destdir = copy.getSourceDir().getIO().getTemp().createTempDirectory();
        variables.put("home", "mhm");
        variables.put("machine", "walter");
        
        assertEquals("", brief(destdir));
        assertEquals("", diff(destdir));

        copy.getSourceDir().join("file").writeLines("home: ${home}", "machine: ${machine}");
        assertEquals("A file\n", brief(destdir));
        assertEquals("### file\n" +
        		"+ home: mhm\n" +
                "+ machine: walter\n", diff(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        
        copy.getSourceDir().join("folder").mkdir();
        assertEquals("A folder\n", brief(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        
        copy.getSourceDir().join("superdir/subdir").mkdirs();
        assertEquals("A superdir\nA superdir/subdir\n", brief(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));

        copy.getSourceDir().join("folder/file").writeLines("home: ${home}\n", "machine: ${machine}\n");
        assertEquals("A folder/file\n", brief(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        
        variables.put("machine", "fritz");
        brief = brief(destdir);
        assertTrue(brief, brief.equals("M folder/file\nM file\n") || brief.equals("M file\nM folder/file\n"));
        normal = diff(destdir);
        assertTrue(normal,
                ("### folder/file\n" +
                "-machine: walter\n" +
                "+machine: fritz\n" +
                "### file\n" +
                "-machine: walter\n" +
                "+machine: fritz\n").equals(normal) ||
                ("### file\n" +
                "-machine: walter\n" +
                "+machine: fritz\n" +
                "### folder/file\n" +
                "-machine: walter\n" +
                "+machine: fritz\n").equals(normal)
                );
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        assertEquals("", diff(destdir));
    }
	
    @Test
    public void templateExt() throws Exception {
        CopyExt foo;
        IO io;
        Node src;
        Node dest;
        Map<String, String> context;
        
        io = new IO();
        src = io.guessProjectHome(getClass()).join("src/test/template");
        dest = io.getTemp().createTempDirectory().join("dest").mkdir();
        context = new HashMap<String, String>();
        context.put("var", "value");
        context.put("name", "foo");
        foo = new CopyExt(src, context);
        foo.directory(dest);
        assertEquals("testdir", foo.called);
        assertEquals("value", dest.join("testfile").readString());
        assertEquals("", dest.join("a").readString());
        assertEquals("value\n", dest.join("b").readString());
        assertEquals("bar", dest.join("foo").readString());
        assertEquals("bar", dest.join("foo").readString());
        assertEquals("1", dest.join("file1").readString());
        assertEquals("2", dest.join("file2").readString());
        assertEquals("11", dest.join("file11").readString());
        assertEquals("12", dest.join("file12").readString());
        assertEquals("21", dest.join("file21").readString());
        assertEquals("22", dest.join("file22").readString());
        assertEquals("1", dest.join("dir1/file").readString());
        assertEquals("2", dest.join("dir2/file").readString());
    }

    private String diff(Node destdir) throws IOException {
        return doDiff(destdir, false);
    }

    private String brief(Node destdir) throws IOException {
        return doDiff(destdir, true);
    }
    
    private String doDiff(Node destdir, boolean brief) throws IOException {
        Node tmp = io.getTemp().createTempDirectory();
        copy.directory(tmp);
        return destdir.diffDirectory(tmp, brief);
    }

    public static class CopyExt extends Copy {
        public String called = null;
        
        public CopyExt(Node srcdir, Map<String, String> variables) {
            super(srcdir, srcdir.getIO().filter().includeAll(), false, variables, Copy.DEFAULT_SUBST, Copy.DEFAULT_SUBST, '-', '@');
        }
        
        public List<Map<String, String>> contextN(Map<String, String> parent) {
            return ctx(parent, "n");
        }

        public List<Map<String, String>> contextMoreNumbers(Map<String, String> parent) {
            return ctx(parent, "m");
        }

        public void callTestDir(Node node, Map<String, String> context) {
            called = node.getName();
        }

        public String callTestFile(Map<String, String> context) {
            return context.get("var");
        }
        
        private List<Map<String, String>> ctx(Map<String, String> parent, String name) {
            List<Map<String, String>> result;
            
            result = new ArrayList<Map<String, String>>();
            result.add(map(parent, name, 1));
            result.add(map(parent, name, 2));
            return result;
        }

        private static Map<String, String> map(Map<String, String> parent, String name, int n) {
            Map<String, String> result;
            
            result = new HashMap<String, String>(parent);
            result.put(name, Integer.toString(n));
            return result;
        }
    }
}
