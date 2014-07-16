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
import net.oneandone.sushi.fs.filter.Tree;
import net.oneandone.sushi.fs.filter.TreeAction;
import net.oneandone.sushi.util.Substitution;

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
    
    protected final Node sourcedir;

    /** applied to sourcedir */
	protected final Filter filter;

    /** content will not be filtered */
    protected final Filter binary;

	private final boolean permissions;
	
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

    public Copy(Node srcdir, Filter filter, Filter binary, boolean modes, Map<String, String> variables, Substitution subst) {
        this(srcdir, filter, binary, modes, variables, subst, subst,
                variables == null ? 0 : DEFAULT_CONTEXT_DELIMITER,
                variables == null ? 0 : DEFAULT_CALL_PREFIX);
    }

    public Copy(Node srcdir, Filter filter,
                boolean permissions, Map<String, String> variables, Substitution path, Substitution content, char contextDelimiter, char callPrefix) {
        this(srcdir, filter, Filter.NOTHING, permissions, variables, path, content, contextDelimiter, callPrefix);
    }

    public Copy(Node srcdir, Filter filter, Filter binary,
                boolean permissions, Map<String, String> variables, Substitution path, Substitution content, char contextDelimiter, char callPrefix) {
	    this.sourcedir = srcdir;
        this.filter = filter;
        this.binary = binary;
        this.permissions = permissions;
		this.path = path;
		this.content = content;
		this.rootVariables = variables;
		this.contextDelimiter = contextDelimiter;
        this.contextConstructors = new HashMap<>();
        this.callPrefix = callPrefix;
        this.calls = new HashMap<>();
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
        
        result = new ArrayList<>();
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
                childVariablesList = new ArrayList<>();
                name = splitContext(name, parentVariables, childVariablesList);
                isDir = src.node.isDirectory();
                for (Map<String, String> childVariables : childVariablesList) {
                    dest = destParent.join(path == null ? name : path.apply(name, childVariables));
                    if (isDir) {
                        dest.mkdirsOpt();
                    } else {
                        dest.getParent().mkdirsOpt();
                        if (content == null || binary.matches(src.node.getRelative(sourcedir))) {
                            src.node.copyFile(dest);
                        } else {
                            dest.writeString(content.apply(src.node.readString(), childVariables));
                        }
                    }
                    if (permissions) {
                        dest.setPermissions(src.node.getPermissions());
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
        
        tmp = new ArrayList<>(contexts);
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
