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
package net.oneandone.sushi.fs.filter;

import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.util.Strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>Similar to java.world.FileFilter or Ant File/Directory sets. A filter is basically a list of paths to
 * include or exclude. Predicates can be used to further restrict the collected nodes. Paths always use
 * slashes (/) - even on Windows - because a) it simplifies writings constants and b) a filter can be
 * applied to any file system.
 *
 * <p>Usage. Create a new instance, use the various selections methods (include, exclude, etc.),
 * and pass the instance for dir.find(). Selection methods return <code>this</code> to allow expressions.</p>
 *
 * <p>A path is a list of names, separated by Filter.SEPARATOR. Paths must *not* start with a
 * separator, i.e they have to be relative. Paths remain relative until the filter is actually
 * applied to a tree. Paths must not end with a separator either. </p>
 *
 * <p>Names use the familiar glob syntax. Filters do not know about extensions. </p>
 */
public class Filter {
    public static final int DEPTH_INFINITE = Integer.MAX_VALUE;

    //--


    /** List of compiled paths. CP = (HEAD, NULL | CP); HEAD = Pattern | String */
    private final List<Object[]> includes;
    private final List<String> includesRepr;

    /** List of compiled paths. */
    private final List<Object[]> excludes;
    private final List<String> excludesRepr;

    private final List<Predicate> predicates;

    private boolean ignoreCase;
    private boolean followLinks;

    private int minDepth;
    private int maxDepth;

    public Filter() {
        this.includes = new ArrayList<>();
        this.includesRepr = new ArrayList<>();
        this.excludes = new ArrayList<>();
        this.excludesRepr = new ArrayList<>();
        this.predicates = new ArrayList<>();
        this.ignoreCase = false;
        this.followLinks = false;
        this.minDepth = 1;
        this.maxDepth = DEPTH_INFINITE;
    }

    public Filter(Filter orig) {
        this.includes = new ArrayList<>(orig.includes);
        this.includesRepr = new ArrayList<>(orig.includesRepr);
        this.excludes = new ArrayList<>(orig.excludes);
        this.excludesRepr = new ArrayList<>(orig.excludesRepr);
        this.predicates = new ArrayList<>(orig.predicates); // TODO: not a deep clone ...
        this.ignoreCase = orig.ignoreCase;
        this.followLinks = orig.followLinks;
        this.minDepth = orig.minDepth;
        this.maxDepth = orig.maxDepth;
    }

    //-- selections methods

    /** Does *not* affect previous calles to include/exclude */
    public Filter ignoreCase() {
        ignoreCase = true;
        return this;
    }

    public Filter followLinks() {
    	followLinks = true;
    	return this;
    }

    public Filter minDepth(int minDepth) {
        this.minDepth = minDepth;
        return this;
    }

    public Filter maxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    public Filter predicate(Predicate p) {
        predicates.add(p);
        return this;
    }

    public Filter includeAll() {
        return includeName("*");
    }

    public Filter include(String... paths) {
        return include(Arrays.asList(paths));
    }

    public Filter include(List<String> paths) {
        for (String path : paths) {
            includes.add(compile(path));
            includesRepr.add(path);
        }
        return this;
    }

    public Filter includeName(String... names) {
        for (String name : names) {
            include(Filesystem.SEPARATOR.join("**", name));
        }
        return this;
    }

    public Filter exclude(String... paths) {
        return exclude(Arrays.asList(paths));
    }

    public Filter exclude(List<String> paths) {
        for (String path : paths) {
            excludes.add(compile(path));
            excludesRepr.add(path);
        }
        return this;
    }

    public Filter excludeName(String... names) {
        for (String name : names) {
            exclude(Filesystem.SEPARATOR.join("**", name));
        }
        return this;
    }

    public String[] getIncludes() {
        return Strings.toArray(includesRepr);
    }

    public String[] getExcludes() {
        return Strings.toArray(excludesRepr);
    }

    public List<Predicate> getPredicates() {
        return predicates;
    }

    //-- select helper methods

    private Object[] compile(String path) {
        List<String> lst;

        lst = Filesystem.SEPARATOR.split(path);
        if (lst.size() == 0) {
            throw new IllegalArgumentException("empty path: " + path);
        }
        if (lst.get(0).equals("")) {
            throw new IllegalArgumentException("absolute path not allowed: " + path);
        }
        if (lst.get(lst.size() - 1).equals("")) {
            throw new IllegalArgumentException(
                "path must not end with separator: " + path);
        }
        return compileTail(lst, 0);
    }

    /**
     * @param lst  array of patterns
     */
    private Object[] compileTail(List<String> lst, int start) {
        Object head;
        Object[] tail;

        if (start == lst.size()) {
            return null;
        } else {
            head = Glob.compile(lst.get(start), ignoreCase);
            tail = compileTail(lst, start + 1);
            if (head == Glob.STARSTAR) {
                if (tail == null) {
                    throw new IllegalArgumentException("** must be followed by some content");
                }
                if (tail[0] == Glob.STARSTAR) {
                    throw new IllegalArgumentException("**/** is not allowed");
                }
            }
        }
        return new Object[] { head, tail };
    }

    public List<Node> collect(Node root) throws IOException {
        List<Node> result;

        result = new ArrayList<>();
        collect(root, result);
        return result;
    }

    public void collect(Node root, List<Node> result) throws IOException {
        invoke(root, new CollectAction(result));
    }

    /**
     * Tests includes an excludes. CAUTION: does not support checks that need a node (like predicates). Ignores "followSymlinks"
     */
    public boolean matches(String path) {
        List<String> segments;

        segments = Filesystem.SEPARATOR.split(path);
        if (segments.size() < minDepth || segments.size() > maxDepth) {
            return false;
        }
        if (predicates.size() > 0) {
            throw new UnsupportedOperationException("cannot match with predicates");
        }
        return matches(0, segments, new ArrayList<>(includes), new ArrayList<>(excludes));
    }

    private boolean matches(int currentSegment, List<String> segments, List<Object[]> includes, List<Object[]> excludes) {
        List<Object[]> remainingIncludes;
        List<Object[]> remainingExcludes;
        String name;
        boolean in;
        boolean ex;

        if (currentSegment >= segments.size()) {
            return false;
        }
        name = segments.get(currentSegment);
        remainingIncludes = new ArrayList<>();
        remainingExcludes = new ArrayList<>();
        in = doMatch(name, includes, remainingIncludes);
        ex = doMatch(name, excludes, remainingExcludes);
        if (in && !ex) {
            return true;
        }
        if (remainingIncludes.size() > 0 && !excludesAll(remainingExcludes)) {
            return matches(currentSegment + 1, segments, remainingIncludes, remainingExcludes);
        } else {
            return false;
        }
    }

    /**
     * Main methods of this class.
     *
     * @throws IOException as thrown by the specified FileTask
     */
    public void invoke(Node root, Action result) throws IOException {
        doInvoke(0, root, root.isLink(), new ArrayList<>(includes), new ArrayList<>(excludes), result);
    }

    private void doInvoke(int currentDepth, Node parent, boolean parentIsLink, List<Object[]> includes, List<Object[]> excludes, Action result)
    throws IOException {
        List<? extends Node> children;
        List<Object[]> remainingIncludes;
        List<Object[]> remainingExcludes;
        String name;
        boolean childIsLink;
        boolean in;
        boolean ex;

        if (currentDepth >= maxDepth) {
            return;
        }
        if (!followLinks && parentIsLink) {
            return;
        }
        try {
            children = list(parent, includes);
        } catch (IOException e) {
            result.enterFailed(parent, parentIsLink, e);
            return;
        }
        if (children == null) {
            // ignore file
        } else {
            result.enter(parent, parentIsLink);
            currentDepth++;
            for (Node child : children) {
                name = child.getName();
                childIsLink = child.isLink();
                remainingIncludes = new ArrayList<>();
                remainingExcludes = new ArrayList<>();
                in = doMatch(name, includes, remainingIncludes);
                ex = doMatch(name, excludes, remainingExcludes);
                if (in && !ex && currentDepth >= minDepth && matchPredicates(child, childIsLink)) {
                    result.select(child, childIsLink);
                }
                if (remainingIncludes.size() > 0 && !excludesAll(remainingExcludes)) {
                    doInvoke(currentDepth, child, childIsLink, remainingIncludes, remainingExcludes, result);
                }
            }
            result.leave(parent, parentIsLink);
        }
    }

    // avoids node.list() call if there is exactly 1 include with a literal head
    private List<? extends Node> list(Node node, List<Object[]> includes) throws IOException {
    	Node child;

    	if (includes.size() == 1 && includes.get(0)[0] instanceof String) {
            child = node.join((String) includes.get(0)[0]);
            if (child.exists()) {
                return Collections.singletonList(child);
            } else {
                return Collections.emptyList();
            }
    	} else {
        	return node.list();
        }
    }

    private boolean matchPredicates(Node node, boolean isLink) throws IOException {
        for (Predicate p : predicates) {
            if (!p.matches(node, isLink)) {
                return false;
            }
        }
        return true;
    }

    private static boolean excludesAll(List<Object[]> excludes) {
        int i;
        int max;
        Object[] pair;
        Object[] tail;

        max = excludes.size();
        for (i = 0; i < max; i++) {
            pair = excludes.get(i);
            tail = (Object[]) pair[1];
            if (pair[0] == Glob.STARSTAR && tail[0] == Glob.STAR) {
                return true;
            }
        }
        return false;
    }

    private static boolean doMatch(String name, List<Object[]> paths, List<Object[]> remainingPaths) {
        boolean found;
        int i;
        int max;
        Object[] path;
        Object head;
        Object[] tail;

        found = false;
        max = paths.size();
        for (i = 0; i < max; i++) {
            path = paths.get(i);
            if (path == null) {
                throw new IllegalStateException("unexpected empty path");
            }
            head = path[0];
            tail = (Object[]) path[1];
            if (head == Glob.STARSTAR) {
                remainingPaths.add(path);
                head = tail[0];
                tail = (Object[]) tail[1];
            }
            if (matches(head, name)) {
                if (tail != null) {
                    remainingPaths.add(tail);
                } else {
                    found = true;
                }
            }
        }
        return found;
    }
    private static boolean matches(Object stringOrPattern, String name) {
    	if (stringOrPattern instanceof String) {
    		return name.equals(stringOrPattern);
    	} else {
    		return Glob.matches((Pattern) stringOrPattern, name);
    	}
    }

    @Override
    public String toString() {
        return "includes=" + includes + ", excludes=" + excludes;
    }
}

