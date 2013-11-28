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
package net.oneandone.sushi.fs;

import net.oneandone.sushi.fs.filter.Filter;
import net.oneandone.sushi.io.OS;
import net.oneandone.sushi.util.Substitution;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CopyDiffTest {
	private final World world;
	private final Map<String, String> variables;
	private final Copy copy;
	
	public CopyDiffTest() throws IOException {
		world = new World();
		variables = new HashMap<>();
        copy = new Copy(world.getTemp().createTempDirectory(),
                world.filter().includeAll(), Filter.NOTHING, world.getTemp().getRoot().getFilesystem().getFeatures().modes,
                variables, Substitution.path(), Substitution.ant(), Copy.DEFAULT_CONTEXT_DELIMITER, Copy.DEFAULT_CALL_PREFIX);
	}

    @Test
    public void mode() throws Exception {
        Node destdir;
        Node file;
        
        if (!copy.getSourceDir().getRoot().getFilesystem().getFeatures().modes) {
        	return;
        }
        destdir = copy.getSourceDir().getWorld().getTemp().createTempDirectory();

        file = copy.getSourceDir().join("file");
        file.writeString("foo");
        file.setPermissions("rwx------");
        copy.directory(destdir);
        assertEquals("rwx------", destdir.join("file").getPermissions());

        file.setPermissions("rw-r-xr-x");
        assertEquals(l("m file"), brief(destdir));
        copy.directory(destdir);
        assertEquals("rw-r-xr-x", destdir.join("file").getPermissions());
    }
    
	@Test
	public void diff() throws Exception {
		Node left;
		Node right;
		
		left = world.getTemp().createTempDirectory();
		right = world.getTemp().createTempDirectory();
		left.join("left").writeString("1");
		right.join("right").writeString("2");
		assertEquals(l("R left", "A right"), left.diffDirectory(right, true));
		assertEquals(l("A right"), new Diff(true).directory(left, right, "right"));
	}

	@Test
    public void template() throws Exception {
        Node destdir;
        String brief;
        String normal;
        
        destdir = copy.getSourceDir().getWorld().getTemp().createTempDirectory();
        variables.put("home", "mhm");
        variables.put("machine", "walter");
        
        assertEquals("", brief(destdir));
        assertEquals("", diff(destdir));

        copy.getSourceDir().join("file").writeLines("home: ${home}", "machine: ${machine}");
        assertEquals(l("A file"), brief(destdir));
        assertEquals(l("### file",
        		"+ home: mhm",
                "+ machine: walter"), diff(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        
        copy.getSourceDir().join("folder").mkdir();
        assertEquals(l("A folder"), brief(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        
        copy.getSourceDir().join("superdir/subdir").mkdirs();
        assertEquals(l("A superdir", "A superdir/subdir"), brief(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));

        copy.getSourceDir().join("folder/file").writeLines("home: ${home}", "machine: ${machine}");
        assertEquals(l("A folder/file"), brief(destdir));
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        
        variables.put("machine", "fritz");
        brief = brief(destdir);
        assertTrue(brief, brief.equals(l("M folder/file", "M file")) || brief.equals(l("M file", "M folder/file")));
        normal = diff(destdir);
        assertTrue(normal,
                (l("### folder/file",
                "-machine: walter",
                "+machine: fritz",
                "### file",
                "-machine: walter",
                "+machine: fritz")).equals(normal) ||
                (l("### file",
                "-machine: walter",
                "+machine: fritz",
                "### folder/file",
                "-machine: walter",
                "+machine: fritz")).equals(normal)
                );
        copy.directory(destdir);
        assertEquals("", brief(destdir));
        assertEquals("", diff(destdir));
    }
	
    @Test
    public void templateExt() throws Exception {
        CopyExt foo;
        World world;
        Node src;
        Node dest;
        Map<String, String> context;
        
        world = new World();
        src = world.guessProjectHome(getClass()).join("src/test/template");
        dest = world.getTemp().createTempDirectory().join("dest").mkdir();
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
        Node tmp = world.getTemp().createTempDirectory();
        copy.directory(tmp);
        return destdir.diffDirectory(tmp, brief);
    }

    public static class CopyExt extends Copy {
        public String called = null;
        
        public CopyExt(Node srcdir, Map<String, String> variables) {
            super(srcdir, srcdir.getWorld().filter().includeAll(), false, variables, Copy.DEFAULT_SUBST, Copy.DEFAULT_SUBST, '-', '@');
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
    
    private static String l(String ... lines) {
    	return OS.CURRENT.lines(lines);
    }
}
