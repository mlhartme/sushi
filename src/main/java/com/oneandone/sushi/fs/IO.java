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

import com.oneandone.sushi.fs.console.ConsoleFilesystem;
import com.oneandone.sushi.fs.file.FileFilesystem;
import com.oneandone.sushi.fs.file.FileNode;
import com.oneandone.sushi.fs.filter.Filter;
import com.oneandone.sushi.fs.memory.MemoryFilesystem;
import com.oneandone.sushi.fs.memory.MemoryNode;
import com.oneandone.sushi.fs.timemachine.TimeMachineFilesystem;
import com.oneandone.sushi.fs.zip.ZipFilesystem;
import com.oneandone.sushi.io.Buffer;
import com.oneandone.sushi.io.OS;
import com.oneandone.sushi.util.Reflect;
import com.oneandone.sushi.util.Strings;
import com.oneandone.sushi.xml.Xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Configures and creates nodes. You'll usually create a single IO instance in your application, configure it and
 * afterwards use it through-out your application to create nodes via IO.node or IO.file. </p>
 *
 * <p>Sushi's FS subsystem forms a tree: An IO object is the root, having filesystems as it's children, roots as
 * grand-children and nodes as leaves. This tree is traversable from nodes up to the IO object via Node.getRoot(),
 * Root.getFilesystem() and Filesystem.getIO(), which is used internally e.g. to pick default encoding settings
 * from IO. (Traversing in reverse order is not implemented - to resource consuming)</p>
 *
 * <p>You can creates as many IO objects as you which, but using nodes from different IO objectes cannot interact
 * so you'll usually stick with a single IO instance.</p>
 *
 * <p>TODO: Multi-Threading. Currently, you need to know fs system internals to properly synchronized
 * multi-threaded applications.</p>
 */
public class IO {
    public final OS os;

    /** never null */
    private final Buffer buffer;

    private final Settings settings;

    /** never null */
    private final Xml xml;

    private Node home;

    /** Intentionally not a file -- see Tempfiles for a rationale */
    private FileNode temp;
    private Node working;

    private final List<String> defaultExcludes;

    private final Map<String, Filesystem> filesystems;
    private final FileFilesystem fileFilesystem;
    private final MemoryFilesystem memoryFilesystem;

    public IO() {
        this(OS.CURRENT, new Settings(), new Buffer(), "**/.svn", "**/.svn/**/*");
        addStandardFilesystems();
    }

    public IO(OS os, Settings settings, Buffer buffer, String... defaultExcludes) {
        this.os = os;
        this.settings = settings;
        this.buffer = buffer;
        this.filesystems = new HashMap<String, Filesystem>();
        this.fileFilesystem = this.addFilesystem(new FileFilesystem(this, "file"));
        this.memoryFilesystem = this.addFilesystem(new MemoryFilesystem(this, "mem"));
        this.temp = init("java.io.tmpdir");
        this.home = init("user.home");
        this.working = init("user.dir");
        this.xml = new Xml();
        this.defaultExcludes = new ArrayList<String>(Arrays.asList(defaultExcludes));
    }

    //-- configuration

    public Node getHome() {
        return home;
    }

    public IO setHome(Node home) {
        this.home = home;
        return this;
    }

    /** current working directory */
    public Node getWorking() {
        return working;
    }

    /** current working directory */
    public IO setWorking(Node working) {
        this.working = working;
        return this;
    }

    public FileNode getTemp() {
        return temp;
    }

    public IO setTemp(FileNode temp) {
        this.temp = temp;
        return this;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public Settings getSettings() {
        return settings;
    }

    public Xml getXml() {
        return xml;
    }

    //--  filesystems

    public IO addStandardFilesystems() {
        addFilesystem(new ConsoleFilesystem(this, "console"));
        addFilesystem(new ZipFilesystem(this, "zip"));
        addFilesystem(new ZipFilesystem(this, "jar"));
        addFilesystem(new TimeMachineFilesystem(this, "tm"));
        addFilesystemOpt("com.oneandone.sushi.fs.ssh.SshFilesystem", this, "ssh");
        addFilesystemOpt("com.oneandone.sushi.fs.svn.SvnFilesystem", this, "svn");
        addFilesystemOpt("com.oneandone.sushi.fs.webdav.WebdavFilesystem", this, "http", "http", false);
        addFilesystemOpt("com.oneandone.sushi.fs.webdav.WebdavFilesystem", this, "https", "https", false);
        addFilesystemOpt("com.oneandone.sushi.fs.webdav.WebdavFilesystem", this, "dav", "http", true);
        addFilesystemOpt("com.oneandone.sushi.fs.webdav.WebdavFilesystem", this, "davs", "https", true);
        return this;
    }

    public <T extends Filesystem> T addFilesystem(T filesystem) {
    	String name;

    	name = filesystem.getScheme();
        if (filesystems.containsKey(name)) {
            throw new IllegalArgumentException("duplicate filesystem scheme: " + name);
        }
        filesystems.put(name, filesystem);
        return filesystem;
    }

    public Filesystem addFilesystemOpt(String filesystemClass, Object ... args) {
        Class<?> clazz;
        Constructor constructor;
        Filesystem filesystem;

        try {
            clazz = Class.forName(filesystemClass);
        } catch (ClassNotFoundException e) {
            return null;
        }
        try {
            constructor = null;
            for (Constructor c : clazz.getConstructors()) {
                if (Reflect.matches(c.getParameterTypes(), args)) {
                    if (constructor != null) {
                        throw new IllegalArgumentException("constructor ambiguous");
                    }
                    constructor = c;
                }
            }
            if (constructor == null) {
                throw new IllegalArgumentException("no constructor: " + filesystemClass);
            }
            filesystem = (Filesystem) constructor.newInstance(args);
            addFilesystem(filesystem);
            return filesystem;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot instantiate " + filesystemClass, e);
        }
    }

    public Filesystem getFilesystem(String scheme) {
        Filesystem result;

        result = filesystems.get(scheme);
        if (result == null) {
            throw new IllegalArgumentException("unknown scheme: " + scheme);
        }
        return result;
    }

    public FileFilesystem getFileFilesystem() {
        return fileFilesystem;
    }

    public MemoryFilesystem getMemoryFilesystem() {
        return memoryFilesystem;
    }

    public <T extends Filesystem> T getFilesystem(String scheme, Class<T> clazz) {
        Filesystem filesystem;

        filesystem = lookupFilesystem(scheme);
        if (filesystem == null) {
            throw new IllegalArgumentException("no such filesystem: " + scheme);
        }
        if (!clazz.isInstance(filesystem)) {
            throw new IllegalArgumentException("unexpected file system type: " + filesystem.getClass().getName());
        }
        return (T) filesystem;
    }

    public Filesystem lookupFilesystem(String scheme) {
        return filesystems.get(scheme);
    }

    //-- Node creation

    public FileNode file(File file) {
        return file(file.getPath());
    }

    public FileNode file(String path) {
        File file;

        if (path.length() > 1) {
            path = Strings.removeEndOpt(path, File.separator);
        }
        file = new File(path);
        if (!file.isAbsolute()) {
            if (working instanceof FileNode) {
                file = new File(((FileNode) working).getFile(), path);
            } else {
                throw new IllegalStateException("working directory is not a file: " + working.getURI());
            }
        }
        return new FileNode(fileFilesystem.getRoot(file), file);
    }

    public Node validNode(String uri) throws NodeInstantiationException {
        try {
            return node(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(uri);
        }
    }

    public Node node(String uri) throws URISyntaxException, NodeInstantiationException {
        return node(new URI(uri));
    }
    public Node node(URI uri) throws NodeInstantiationException {
        return node(uri, null);
    }

    public Node node(URI uri, Object extra) throws NodeInstantiationException {
        String scheme;
        Filesystem fs;
        Node base;
        Node result;

        if (uri.isAbsolute()) {
            base = null;
        } else {
            uri = working.getURI().resolve(uri);
            base = working;
        }
        scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalStateException("missing scheme: " + uri);
        }
        fs = filesystems.get(scheme);
        if (fs == null) {
            throw new NodeInstantiationException(uri, "unknown scheme: " + scheme);
        }
        result = fs.node(uri, extra);
        if (base != null) {
            result.setBase(base);
        }
        return result;
    }

    private MemoryNode memoryNode() {
        // TODO: re-use root?
        // TODO: when delete?
        return memoryFilesystem.root().node("tmp", null);
    }

    public MemoryNode memoryNode(String content) {
        try {
            return (MemoryNode) memoryNode().writeString(content);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public MemoryNode memoryNode(byte ... bytes) {
        try {
            return (MemoryNode) memoryNode().writeBytes(bytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** @param name must not start with a slash */
    public Node resource(String name) throws IOException {
        List<Node> result;

        result = resources(name);
        switch (result.size()) {
        case 0:
            throw new FileNotFoundException("no such resource: " + name);
        case 1:
            return result.get(0);
        default:
            throw new IOException("resource ambiguous: " + name + "(" + result + ")");
        }
    }

    /**
     * Throws an IllegalStateException is the classpath contains duplicate items
     * @param name must not start with a slash
     */
    public List<Node> resources(String name) throws IOException {
        return resources(name, true);
    }

    /** @param name must not start with a slash */
    public List<Node> resourcesUnchecked(String name) throws IOException {
        return resources(name, false);
    }

    /** @param name must not start with a slash */
    private List<Node> resources(String name, boolean rejectDuplicates) throws IOException {
        Enumeration<URL> e;
        List<Node> result;
        Node add;

        if (name.startsWith("/")) {
            throw new IllegalArgumentException();
        }
        e = getClass().getClassLoader().getResources(name);
        result = new ArrayList<Node>();
        while (e.hasMoreElements()) {
            try {
                add = node(e.nextElement().toURI());
            } catch (URISyntaxException ex) {
                throw new IllegalStateException(ex);
            }
            if (result.contains(add)) {
                if (rejectDuplicates) {
                    throw new IllegalStateException("duplicate classpath item: " + add);
                }
            } else {
                result.add(add);
            }
        }
        return result;
    }

    //--

    public Filter filter() {
        Filter filter;

        filter = new Filter();
        filter.exclude(defaultExcludes);
        return filter;
    }

    public List<String> defaultExcludes() {
        return defaultExcludes;
    }

    //--

    public List<Node> path(String path) throws URISyntaxException, NodeInstantiationException {
        List<Node> result;

        result = new ArrayList<Node>();
        for (String str: Strings.split(os.listSeparator, path)) {
            result.add(node(str));
        }
        return result;
    }

    public List<Node> classpath(String path) throws URISyntaxException, IOException {
        List<Node> result;

        result = path(path);
        for (Node node : result) {
            node.checkExists();
        }
        return result;
    }

    //-- classpath

    /**
     * Returns the file or directory containing the specified resource.
     */
    public FileNode locateClasspathItem(String resourcename) {
        return locateClasspathItem(getClass(), resourcename);
    }

    /**
     * Returns the file or directory containing the specified class.
     *
     * @param c the source class
     *
     * @return the physical file defining the class
     */
    public FileNode locateClasspathItem(Class<?> c) {
        return locateClasspathItem(c, Reflect.resourceName(c));
    }

    /** Throws a RuntimeException if the resource is not found */
    public FileNode locateClasspathItem(Class<?> base, String resourcename) {
        URL url;
        FileNode file;

        url = base.getResource(resourcename);
        if (url == null) {
            throw new RuntimeException("no such resource: " + resourcename);
        }
        file = locateClasspathItem(url, resourcename);
        if (!file.exists()) {
            throw new RuntimeException(url + ": no such file or directory: " + file);
        }
        file = locateClasspathItem(url, resourcename);
        if (!file.exists()) {
            throw new RuntimeException(url + ": no such file or directory: " + file);
        }
        return file;
    }

    public FileNode guessProjectHome(Class<?> c) {
        FileNode node;

        node = locateClasspathItem(c);
        if (node.isDirectory()) {
            if (node.getName().endsWith("classes")) {
                node = (FileNode) node.getParent();
            }
        } else {
            if (node.getName().endsWith(".jar")) {
                node = (FileNode) node.getParent();
            }
        }
        if (node.getName().endsWith("target")) {
            node = (FileNode) node.getParent();
        }
        return node;
    }

    /**
     * Returns the file of a certain class at a special location. e.g. jar files
     *
     * @param url the destination path to the resource
     * @param resourcename  absolute resource name; redundant, but necessary to strip from urls
     *
     * @return the physical file referring to the class
     */
    public FileNode locateClasspathItem(URL url, String resourcename) {
        String filename;
        FileNode file;
        String protocol;
        int idx;

        if (!resourcename.startsWith("/")) {
            throw new IllegalArgumentException("absolute resourcename expected: " + resourcename);
        }
        protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            try {
                file = file(new File(url.toURI()));
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }
            filename = file.getAbsolute();
            if (!filename.endsWith(resourcename.replace('/', File.separatorChar))) {
                throw new RuntimeException("classname not found in file url: " + filename + " " + resourcename);
            }
            file = file(filename.substring(0, filename.length() - resourcename.length()));
        } else if ("jar".equals(protocol)) {
            // obtaining the jar file follows the code in java.net.JarURLConnection
            filename = url.getFile();
            filename = Strings.removeStart(filename, "file:");
            idx = filename.indexOf("!/");
            if (idx == -1) {
                throw new RuntimeException("!/ not found: " + filename);
            }
            file = file(filename.substring(0, idx));
        } else {
            throw new RuntimeException("protocol not supported: " + protocol);
        }
        return file;
    }

    //--

    private FileNode init(String name) {
        String value;
        File file;

        value = System.getProperty(name);
        if (value == null) {
            throw new IllegalStateException("property not found: " + name);
        }
        file = new File(value);
        if (!file.isDirectory()) {
            throw new IllegalStateException(
                "property " + name + " does not point to a directory: " + value);
        }
        return file(file);
    }
}
