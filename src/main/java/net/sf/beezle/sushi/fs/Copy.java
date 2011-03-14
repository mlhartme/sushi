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

package net.sf.beezle.sushi.fs;

import net.sf.beezle.sushi.fs.filter.Filter;
import net.sf.beezle.sushi.fs.filter.Tree;
import net.sf.beezle.sushi.fs.filter.TreeAction;
import net.sf.beezle.sushi.util.Substitution;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Copy configuration and command. */
public class Copy {
    public static final char DEFAULT_CONTEXT_DELIMITER = ':';
    public static final char DEFAULT_CALL_PREFIX = '@';
    public static final Substitution DEFAULT_SUBST = new Substitution("${{", "}}", '\\');
    
    private static final String CONTEXT = "context";
    private static final String CALL = "call";
    
    private final Node sourcedir;

    /** applied to sourcedir */
	private final Filter filter;

	private final boolean modes;
	
    private final Substitution path;
    private final Substitution content;
    private final Map<String, String> rootVariables;
    
    private final char contextDelimiter;
    private final Map<Character, Method> contextConstructors;
    private final char callPrefix;
    private final Map<String, Method> calls;
    
	public Copy(Node srcdir) {
		this(srcdir, srcdir.getWorld().filter().includeAll());
	}
	
    public Copy(Node srcdir, Filter filter) {
        this(srcdir, filter, false);
    }
    
    public Copy(Node srcdir, Filter filter, boolean modes) {
        this(srcdir, filter, modes, null, null);
    }
    
    public Copy(Node srcdir, Filter filter, boolean modes, Map<String, String> variables) {
        this(srcdir, filter, modes, variables, variables == null ? null : DEFAULT_SUBST);
    }

    public Copy(Node srcdir, Filter filter, boolean modes, Map<String, String> variables, Substitution subst) {
        this(srcdir, filter, modes, variables, subst, subst, 
                variables == null ? 0 : DEFAULT_CONTEXT_DELIMITER, 
                variables == null ? 0 : DEFAULT_CALL_PREFIX);
    }

    public Copy(Node srcdir, Filter filter, boolean modes, Map<String, String> variables, Substitution path, Substitution content, char contextDelimiter, char callPrefix) {
	    this.sourcedir = srcdir;
        this.filter = filter;
        this.modes = modes;
		this.path = path;
		this.content = content;
		this.rootVariables = variables;
		this.contextDelimiter = contextDelimiter;
        this.contextConstructors = new HashMap<Character, Method>();
        this.callPrefix = callPrefix;
        this.calls = new HashMap<String, Method>();
        if (!getClass().equals(Copy.class)) {
            initReflection();
        }
    }
    
    private void initReflection() {
        String name;
        char c;
        
        for (Method m : getClass().getDeclaredMethods()) {
            name = m.getName();
            if (name.startsWith(CONTEXT)) {
                c = Character.toUpperCase(name.substring(CONTEXT.length()).charAt(0));                
                if (contextConstructors.put(c, m) != null) {
                    throw new IllegalArgumentException("duplicate context character: " + c);
                }
            } else if (name.startsWith(CALL)) {
                name = name.substring(CALL.length()).toLowerCase();
                if (calls.put(name, m) != null) {
                    throw new IllegalArgumentException("duplicate call: " + name);
                }
            }
        }
    }

	public Node getSourceDir() {
	    return sourcedir;
	}
	
	/** @return Target files or directories created. */
	public List<Node> directory(Node destdir) throws CopyException {
        List<Node> result;
        TreeAction action;
        Tree tree;
        
        result = new ArrayList<Node>();
        try {
            sourcedir.checkDirectory();
            destdir.checkDirectory();
            action = new TreeAction();
            filter.invoke(sourcedir, action);
        } catch (IOException e) {
            throw new CopyException(sourcedir, destdir, "scanning source files failed", e);
        }
		tree = action.getResult();
		if (tree != null) {
		    for (Tree child : tree.children) {
		        copy(sourcedir, destdir, child, result, rootVariables);
		    }
		}
		return result;
	}
	
	private void copy(Node srcParent, Node destParent, Tree src, List<Node> result, Map<String, String> parentVariables) throws CopyException {
	    String name;
        Node dest;
        List<Map<String, String>> childVariablesList;
        boolean isDir;
        
        name = src.node.getName();
        dest = null;
        try {
            if (callPrefix != 0 && name.length() > 0 && name.charAt(0) == callPrefix) {
                result.add(call(name, src.node, destParent, parentVariables));
            } else {
                childVariablesList = new ArrayList<Map<String, String>>();
                name = splitContext(name, parentVariables, childVariablesList);
                isDir = src.node.isDirectory();
                for (Map<String, String> childVariables : childVariablesList) {
                    dest = destParent.join(path == null ? name : path.apply(name, childVariables));
                    if (isDir) {
                        dest.mkdirsOpt();
                    } else {
                        dest.getParent().mkdirsOpt();
                        if (content != null) {
                            dest.writeString(content.apply(src.node.readString(), childVariables));
                        } else {
                            src.node.copyFile(dest);
                        }
                    }
                    if (modes) {
                        dest.setMode(src.node.getMode());
                    }
                    result.add(dest);
                    for (Tree child : src.children) {
                        copy(src.node, dest, child, result, childVariables);
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (dest == null) {
                dest = destParent.join(name);
            }
            throw new CopyException(src.node, dest, e);
        }
	}

    private Node call(String name, Node src, Node destParent, Map<String, String> context) throws ReflectionException, IOException {
        String fileName;
        String methodName;
        Method m;
        Node dest;
        
        fileName = name.substring(1);
        methodName = normalize(fileName);
        m = calls.get(methodName);
        if (m == null) {
            throw new ReflectionException("unknown call: " + methodName + " (defined: " + calls.keySet() + ")");
        }
        dest = destParent.join(fileName);
        if (src.isDirectory()) {
            dest.mkdirsOpt();
            doInvoke(m, dest, context);
        } else {
            dest.writeString((String) doInvoke(m, context));
        }
        return dest;
    }

    private static String normalize(String str) {
        StringBuilder builder;
        char ch;
        
        builder = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            ch = str.charAt(i);
            if (Character.isJavaIdentifierPart(ch)) {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString();
    }
    
    private String splitContext(String name, Map<String, String> parent, List<Map<String, String>> result) throws ReflectionException {
        int idx;
        char c;
        Method m;
        
        result.add(parent);
        if (contextDelimiter == 0) {
            return name;
        }
        idx = name.indexOf(contextDelimiter);
        if (idx == -1) {
            return name;
        }
        for (int i = 0; i < idx; i++) {
            c = name.charAt(i);
            m = contextConstructors.get(name.charAt(i));
            if (m == null) {
                throw new ReflectionException("unknown context: " + c + " (defined: " + contextConstructors.keySet() + ")");
            }
            apply(m, result);
        }
        return name.substring(idx + 1);
    }
    
    private void apply(Method m, List<Map<String, String>> contexts) throws ReflectionException {
        List<Map<String, String>> tmp;
        
        tmp = new ArrayList<Map<String, String>>(contexts);
        contexts.clear();
        for (Map<String, String> map : tmp) {
            context(m, map, contexts);
        }
    }

    private void context(Method m, Map<String, String> parent, List<Map<String, String>> result) throws ReflectionException {
        result.addAll((List<Map<String, String>>) doInvoke(m, parent));
    }

    private Object doInvoke(Method m, Object ... args) throws ReflectionException {
        try {
            return m.invoke(this, args);
        } catch (InvocationTargetException e) {
            throw new ReflectionException(m.getName() + " failed: " + e.getTargetException().getMessage(), e.getTargetException());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(m.getName() + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
