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

package com.oneandone.sushi.fs.filter;

import com.oneandone.sushi.fs.Node;
import com.oneandone.sushi.util.Strings;

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
 * <p>Names use the familar glob syntax. Filters do not know about extensions. </p>
 */
public class Filter {
    public static final int DEPTH_INFINITE = Integer.MAX_VALUE;
    
    public static final char SEPARATOR_CHAR = '/';
    public static final String SEPARATOR = "" + SEPARATOR_CHAR;

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
        this.includes = new ArrayList<Object[]>();
        this.includesRepr = new ArrayList<String>();
        this.excludes = new ArrayList<Object[]>();
        this.excludesRepr = new ArrayList<String>();
        this.predicates = new ArrayList<Predicate>();
        this.ignoreCase = false;
        this.followLinks = false;
        this.minDepth = 1;
        this.maxDepth = DEPTH_INFINITE;
    }
    
    public Filter(Filter orig) {
        this.includes = new ArrayList<Object[]>(orig.includes);
        this.includesRepr = new ArrayList<String>(orig.includesRepr);
        this.excludes = new ArrayList<Object[]>(orig.excludes);
        this.excludesRepr = new ArrayList<String>(orig.excludesRepr);
        this.predicates = new ArrayList<Predicate>(orig.predicates); // TODO: not a deep clone ...
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
            include(Strings.join(SEPARATOR, "**", name));
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
            exclude(Strings.join(SEPARATOR, "**", name));
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
        
        lst = Strings.split(SEPARATOR, path);
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
        
        result = new ArrayList<Node>();
        collect(root, result);
        return result;
    }
    
    public void collect(Node root, List<Node> result) throws IOException {
        invoke(root, new CollectAction(result));
    }

    /**
     * Main methods of this class.
     *
     * @throws IOException as thrown by the specified FileTask
     */
    public void invoke(Node root, Action result) throws IOException {
        doInvoke(0, root, root.isLink(), new ArrayList<Object[]>(includes), new ArrayList<Object[]>(excludes), result);
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
                remainingIncludes = new ArrayList<Object[]>();
                remainingExcludes = new ArrayList<Object[]>();
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

    // avoids node.list() call with there is exactly 1 include with a literal head
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

