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
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Diff {
    private final boolean brief;
	private final String lineSeparator;

    public Diff(boolean brief) {
    	this(brief, OS.CURRENT.lineSeparator.getSeparator());
    }

    public Diff(boolean brief, String lineSeparator) {
        this.brief = brief;
        this.lineSeparator = lineSeparator;
    }

    //-- scan directories for relevant files

    public List<String> paths(Node dir, Filter filter) throws IOException {
    	List<String> result;

    	result = new ArrayList<>();
    	paths(dir, filter, result);
    	return result;
    }

    public void paths(Node dir, Filter filter, List<String> result) throws IOException {
    	String path;

    	for (Node node : dir.find(filter)) {
    		path = node.getRelative(dir);
    		if (!result.contains(path)) {
    			result.add(path);
    		}
    	}
    }

    //-- diff

    public String directory(Node leftdir, Node rightdir, Filter filter) throws IOException {
    	List<String> paths;

    	paths = paths(leftdir, filter);
    	paths(rightdir, filter, paths);
    	return directory(leftdir, rightdir, paths);
    }

    public String directory(Node leftdir, Node rightdir, String ... paths) throws IOException {
    	return directory(leftdir, rightdir, Arrays.asList(paths));
    }

    public String directory(Node leftdir, Node rightdir, List<String> paths) throws IOException {
        StringBuilder result;
        Node left;
        Node right;

        result = new StringBuilder();
        leftdir.checkDirectory();
        rightdir.checkDirectory();
        for (String path : paths) {
            left = leftdir.join(path);
            right = rightdir.join(path);
            if (left.isDirectory()) {
                if (right.isDirectory()) {
                    // ok
                } else if (right.isFile()) {
                    throw new IOException("TODO");
                } else {
                    if (brief) {
                        header('A', path, result);
                    } else {
                        // TODO
                    }
                }
            } else if (right.isDirectory()) {
                header("A", path, result);
            } else {
                file(left, right, path, result);
            }
        }
        return result.toString();
    }

    public void file(Node left, Node cmp, String relative, StringBuilder result) throws IOException {
        if (brief) {
            header(left, cmp, relative, result);
        } else {
            fileNormal(left, cmp, relative, result);
        }
    }

    public void fileNormal(Node left, Node right, String relative, StringBuilder result) throws IOException {
        String str;

        if (!left.exists()) {
            right.checkFile();
            header("###", relative, result);
            result.append(Strings.indent(right.readString(), "+ "));
        } else {
            str = net.oneandone.sushi.util.Diff.diff(left.readString(), right.readString());
            if (str.length() > 0) {
                header("###", relative, result);
                result.append(str);
            }
        }
    }

    public void header(Node left, Node right, String relative, StringBuilder result) throws IOException {
        if (!left.exists()) {
            right.checkFile();
            header('A', relative, result);
        } else if (!right.exists()) {
            header('R', relative, result);
        } else if (left.diff(right)) {
            header('M', relative, result);
        } else if (left.getRoot().getFilesystem().getFeatures().modes
        		&& !left.getPermissions().equals(right.getPermissions())) {
            header('m', relative, result);
        } else {
            // nothing
        }
    }

    private void header(char name, String relative, StringBuilder result) {
        header(Character.toString(name), relative, result);
    }

    private void header(String name, String relative, StringBuilder result) {
        result.append(name).append(' ').append(relative).append(lineSeparator);
    }
}
