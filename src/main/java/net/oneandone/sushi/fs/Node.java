/*
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
import net.oneandone.sushi.io.Buffer;
import net.oneandone.sushi.io.LineFormat;
import net.oneandone.sushi.io.LineReader;
import net.oneandone.sushi.util.Strings;
import net.oneandone.sushi.xml.Builder;
import net.oneandone.sushi.xml.Serializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * <p>Abstraction from a file: something stored under a path that you can get an input stream from or output stream to.
 * FileNode is the most prominent example of a node. The api is similar to java.world.File. It provides the
 * same functionality, adds some methods useful for scripting, and removes some redundant methods to simplify
 * api (in particular the constructors). </p>
 *
 * <p>A node is identified by a URI, whose most important part is the path. </p>
 *
 * <p>The path is a sequence of names separated by /, even for files on windows. It never starts
 * or ends with a separator. It does not include the root, but it always includes the path
 * of the base. A node with an empty path is called root node. That last part of the path is the name.
 * Paths are always specified decoded, but URIs always contain encoded paths.
 *
 * <p>The base is a node this node is relative to. It's optional, a node without base is called absolute.
 * It's use to simplify (shorten!) toString output.</p>
 *
 * <p>Your application usually creates some starting nodes with <code>world.node(URI)</code> or <code>world.file(str)</code>.
 * They will be used to create actual working nodes with <code>node.join(path)</code>. The constructor
 * of the respective node class is rarely used directly, it's used indirectly by the filesystem. </p>
 *
 * <p>A node is immutable, except for its base.</p>
 *
 * <p>Method names try to be short, but no abbreviations. Exceptions from this rule are mkfile, mkdir and mklink, because
 * mkdir is a well-established name.</p>
 *
 * <p>If an Implementation cannot (or does not want to) implement a method (e.g. move), it throws an
 * UnsupportedOperationException.</p>
 *
 * <p>You can read nodes using traditional stream or reader/writers. In addition to this pull-logic, the readTo method
 * provides push logic. Some Node implementations are more efficient when using readTo() (whis is indicated the the inverseIO feature).
 * I'd appreciate if all underlying libraries provided pull logic because, push logic can be efficiently implemented on top ... </p>
 *
 * <p>As long as you stick to read operations, nodes are thread-save.</p>
 *
 * <p>Exception handling: throws NodeNotFoundException, FileNotFoundException, DirectoryNotFoundException to indicate
 * a node, file or directory is expected to exist, but it does not.</p>
 */
public abstract class Node<T extends Node> {
    protected UnsupportedOperationException unsupported(String op) {
        return new UnsupportedOperationException(this + ":" + op);
    }

    //--

    public abstract Root<T> getRoot();

    public T getRootNode() {
        return getRoot().node("", null);
    }

    public World getWorld() {
        return getRoot().getFilesystem().getWorld();
    }

    //-- stream, reader, writer

    public NodeReader newReader() throws IOException {
        return NodeReader.create(this);
    }

    public ObjectInputStream newObjectInputStream() throws IOException {
        return new ObjectInputStream(newInputStream());
    }

    public NodeWriter newWriter() throws IOException {
        return newWriter(false);
    }

    public NodeWriter newAppender() throws IOException {
        return newWriter(true);
    }

    public NodeWriter newWriter(boolean append) throws IOException {
        return NodeWriter.create(this, append);
    }

    public ObjectOutputStream newObjectOutputStream() throws IOException {
        return new ObjectOutputStream(newOutputStream());
    }

    public OutputStream newOutputStream() throws NewOutputStreamException {
        return newOutputStream(false);
    }

    public OutputStream newAppendStream() throws NewOutputStreamException {
        return newOutputStream(true);
    }

    /**
     * Create a stream to write this node.
     * Closing the stream more than once is ok, but writing to a closed stream is rejected by an exception.
     * @throws NewDirectoryOutputStreamException if this node is a directory
     * @throws NewOutputStreamException for other problems creating the stream
     */
    public abstract OutputStream newOutputStream(boolean append) throws NewOutputStreamException;

    public LineReader newLineReader() throws IOException {
        return newLineReader(getWorld().getSettings().lineFormat);
    }

    public LineReader newLineReader(LineFormat format) throws IOException {
        return new LineReader(newReader(), format);
    }

    /**
     * Creates a stream to read this node.
     * Closing the stream more than once is ok, but reading from a closed stream is rejected by an exception
     */
    public abstract InputStream newInputStream() throws FileNotFoundException, NewInputStreamException;

    public FilterInputStream newInputStreamDeleteOnClose() throws FileNotFoundException, NewInputStreamException {
        return new FilterInputStream(newInputStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                // opt because it may be closed twice:
                deleteFileOpt();
            }
        };
    }

    //-- directories

    /**
     * Lists child nodes of this node.
     * @return List of child nodes or null if this node is a file. Note that returning null allows for optimizations
     *    because list() may be called on any existing node; otherwise, you'd have to inspect the resulting exception
     *    whether you called list on a file.
     * @throws ListException if this does not exist (in this case, cause is set to a FileNotFoundException),
     *    permission is denied, or another IO problem occurs.
     */
    public abstract List<T> list() throws ListException, DirectoryNotFoundException;

    /**
     * Fails if the directory already exists. Features define whether is operation is atomic.
     * @return this
     */
    public abstract T mkdir() throws MkdirException;

    public T mkdirOpt() throws MkdirException {
        try {
            if (!isDirectory()) {
                mkdir(); // fail here if it's a file!
            }
        } catch (ExistsException e) {
            throw new MkdirException(this, e);
        }
        return (T) this;
    }

    public T mkdirsOpt() throws MkdirException {
        T parent;

        try {
            if (!isDirectory()) {
                parent = getParent();
                if (parent != null) {
                    parent.mkdirsOpt();
                }
                mkdir(); // fail here if it's a file!
            }
        } catch (ExistsException e) {
            throw new MkdirException(this, e);
        }
        return (T)this;
    }

    public T mkdirs() throws MkdirException {
        try {
            if (exists()) {
                throw new MkdirException(this);
            }
            return mkdirsOpt();
        } catch (IOException e) {
            throw new MkdirException(this, e);
        }
    }

    //-- delete

    /**
     * Deletes this node, no matter if it's a file or a directory or a broken link. If this is a link, the link is deleted, not the link target.
     *
     * @return this
     */
    public abstract T deleteTree() throws NodeNotFoundException, DeleteException;

    /**
     * Deletes this file or link to a file; throws an exception otherwise.
     * CAUTION: does not delete dangling link - in this case, use deleteTree.
     *
     * @throws DeleteException if this is not file
     */
    public abstract T deleteFile() throws FileNotFoundException, DeleteException;

    /**
     * Deletes this directory or link to a directory; throws an exception otherwise.
     *
     * @throws DeleteException if this is not a directory or the directory is not empty
     */
    public abstract T deleteDirectory() throws DirectoryNotFoundException, DeleteException;

    public T deleteFileOpt() throws IOException {
        if (exists()) {
            deleteFile();
        }
        return (T) this;
    }

    public T deleteDirectoryOpt() throws IOException {
        if (exists()) {
            deleteDirectory();
        }
        return (T) this;
    }

    public T deleteTreeOpt() throws IOException {
        if (exists()) {
            deleteTree();
        }
        return (T) this;
    }

    //-- status methods

    /** Throws a LengthException if this node is not a file. */
    public abstract long size() throws SizeException;

    /** Throws an exception is the file does not exist */
    public abstract long getLastModified() throws GetLastModifiedException;
    public abstract void setLastModified(long millis) throws SetLastModifiedException;

    public abstract String getPermissions() throws ModeException;
    public abstract void setPermissions(String permissions) throws ModeException;

    public abstract UserPrincipal getOwner() throws ModeException;
    public abstract void setOwner(UserPrincipal owner) throws ModeException;
    public abstract GroupPrincipal getGroup() throws ModeException;
    public abstract void setGroup(GroupPrincipal group) throws ModeException;

    /**
     * Tests if this is a file, directory or link.
     * @return true if the node exists, even if it's a dangling link.
     */
    public abstract boolean exists() throws ExistsException;

    /** @return true for files and links to files */
    public abstract boolean isFile() throws ExistsException;

    /** @return true for directories and links to directories */
    public abstract boolean isDirectory() throws ExistsException;

    /** @return true for links to files or directories or dangling links */
    public abstract boolean isLink() throws ExistsException;

    public T checkExists() throws ExistsException, NodeNotFoundException {
        if (!exists()) {
            throw new NodeNotFoundException(this);
        }
        return (T) this;
    }

    public T checkNotExists() throws ExistsException, NodeAlreadyExistsException {
        if (exists()) {
            throw new NodeAlreadyExistsException(this);
        }
        return (T) this;
    }

    public T checkDirectory() throws ExistsException, DirectoryNotFoundException {
        if (isDirectory()) {
            return (T) this;
        }
        if (exists()) {
            throw new DirectoryNotFoundException(this, "directory not found - this is a file");
        } else {
            throw new DirectoryNotFoundException(this);
        }
    }

    /** @return false for dangling links */
    public T checkFile() throws ExistsException, FileNotFoundException {
        if (isFile()) {
            return (T) this;
        }
        if (exists()) {
            throw new FileNotFoundException(this, "file not found - this is a directory");
        } else {
            throw new FileNotFoundException(this);
        }
    }

    //-- path functionality

    /**
     * Never starts or end with a slash or a drive; an empty string is the root path. The path is decoded,
     * you have to encoded if you want to build an URI.
     */
    public abstract String getPath();

    /**
     * @return a normalized URI, not necessarily the URI this node was created from. Does not contain userInfo to avoid accidentialls
     * printing it. Use getUriWithUserInfo instead if you know what you're doing. */
    public URI getUri() {
        return URI.create(getRoot().getFilesystem().getScheme() + ":" + getRoot().getId() + encodePath(getPath()));
    }

    /** Override this if your root might contain userInfo. */
    public URI getUriWithUserInfo() {
        return getUri();
    }


    /** @return the last path segment (or an empty string for the root node */
    public String getName() {
        String path;

        path = getPath();
        // ok for -1:
        return path.substring(path.lastIndexOf(Filesystem.SEPARATOR_CHAR) + 1);
    }

    /** @return last extension of the file name, without the dot. */
    public String getExtension() {
        String name;
        int idx;

        name = getName();
        idx = name.lastIndexOf('.');
        if (idx <= 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1);
    }


    public T getParent() {
        String path;
        int idx;

        path = getPath();
        if ("".equals(path)) {
            return null;
        }
        idx = path.lastIndexOf(Filesystem.SEPARATOR_CHAR);
        if (idx == -1) {
            return getRoot().node("", null);
        } else {
            return getRoot().node(path.substring(0, idx), null);
        }
    }

    public boolean hasDifferentAncestor(Node ancestor) {
        T parent;

        parent = getParent();
        if (parent == null) {
            return false;
        } else {
            return parent.hasAncestor(ancestor);
        }
    }

    public boolean hasAncestor(Node ancestor) {
        T current;

        current = (T) this;
        while (true) {
            if (current.equals(ancestor)) {
                return true;
            }
            current = (T) current.getParent();
            if (current == null) {
                return false;
            }
        }
    }

    /** @return kind of a path, with . and .. where appropriate. */
    public String getRelative(Node base) {
        String startfilepath;
        String destpath;
        String common;
        StringBuilder result;
        int len;
        int ups;
        int i;

        if (base.equals(this)) {
            return ".";
        }
        startfilepath = base.join("foo").getPath();
        destpath = getPath();
        common = Strings.getCommon(startfilepath, destpath);
        common = common.substring(0, common.lastIndexOf(Filesystem.SEPARATOR_CHAR) + 1);  // ok for idx == -1
        len = common.length();
        startfilepath = startfilepath.substring(len);
        destpath = destpath.substring(len);
        result = new StringBuilder();
        ups = Strings.count(startfilepath, Filesystem.SEPARATOR_STRING);
        for (i = 0; i < ups; i++) {
            result.append("..").append(Filesystem.SEPARATOR_STRING);
        }
        result.append(destpath);
        return result.toString();
    }

    public T join(List<String> paths) {
        Root<T> root;
        T result;

        root = getRoot();
        result = root.node(root.getFilesystem().join(getPath(), paths), null);
        return result;
    }

    public T join(String... names) {
        return join(Arrays.asList(names));
    }

    //-- read functionality

    /**
     * Reads all bytes of the node.
     *
     * Default implementation that works for all nodes: reads the file in chunks and builds the result in memory.
     * Derived classes should override it if they can provide a more efficient implementation, e.g. by determining
     * the length first if getting the length is cheap.
     */
    public byte[] readBytes() throws IOException {
        Buffer buffer;

        try (InputStream src = newInputStream()) {
            buffer = getWorld().getBuffer();
            synchronized (buffer) {
                return buffer.readBytes(src);
            }
        }
    }

    /**
     * Reads all chars of the node.  Do not use this method on large files because it's memory consuming: the string
     * is created from the byte array returned by readBytes.
     */
    public String readString() throws IOException {
        return getWorld().getSettings().string(readBytes());
    }

    /** @return lines without tailing line separator */
    public List<String> readLines() throws IOException {
        return readLines(getWorld().getSettings().lineFormat);
    }

    /** @return lines without tailing line separator */
    public List<String> readLines(LineFormat format) throws IOException {
        return newLineReader(format).collect();
    }

    /** Reads properties with the encoding for this node */
    public Properties readProperties() throws IOException {
        Properties p;

        try (Reader src = newReader()) {
            p = new Properties();
            p.load(src);
        }
        return p;
    }

    public Object readObject() throws IOException {
        Object result;

        try (ObjectInputStream src = newObjectInputStream()) {
            result = src.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public Document readXml() throws IOException, SAXException {
        Builder builder;

        builder = getWorld().getXml().getBuilder();
        synchronized (builder) {
            return builder.parse(this);
        }
    }

    public Transformer readXsl() throws IOException, TransformerConfigurationException {
        Templates templates;

        try (InputStream in = newInputStream()) {
            templates = Serializer.templates(new SAXSource(new InputSource(in)));
        }
        return templates.newTransformer();
    }

    //-- write functionality

    /**
     * Fails if the file already exists. Features define whether this operation is atomic.
     * This default implementation is not atomic.
     * @return this
     */
    public T mkfile() throws MkfileException {
        try {
            if (exists()) {
                throw new MkfileException(this);
            }
            return writeBytes();
        } catch (IOException e) {
            throw new MkfileException(this, e);
        }
    }

    public T writeBytes(byte... bytes) throws IOException {
        return writeBytes(bytes, 0, bytes.length, false);
    }

    public T appendBytes(byte... bytes) throws IOException {
        return writeBytes(bytes, 0, bytes.length, true);
    }

    public T writeBytes(byte[] bytes, int ofs, int len, boolean append) throws IOException {
        try (OutputStream out = newOutputStream(append)) {
            out.write(bytes, ofs, len);
        }
        return (T) this;
    }

    public T writeChars(char... chars) throws IOException {
        return writeChars(chars, 0, chars.length, false);
    }

    public T appendChars(char... chars) throws IOException {
        return writeChars(chars, 0, chars.length, true);
    }

    public T writeChars(char[] chars, int ofs, int len, boolean append) throws IOException {
        try (Writer out = newWriter(append)) {
            out.write(chars, ofs, len);
        }
        return (T) this;
    }

    public T writeString(String txt) throws IOException {
        try (Writer w = newWriter()) {
            w.write(txt);
        }
        return (T) this;
    }

    public T appendString(String txt) throws IOException {
        try (Writer w = newAppender()) {
            w.write(txt);
        }
        return (T) this;
    }

    public T writeStrings(String... str) throws IOException {
        return writeStrings(Arrays.asList(str));
    }

    public T writeStrings(List<String> strings) throws IOException {
        return strings(newWriter(), strings);
    }

    public T appendStrings(String... str) throws IOException {
        return appendStrings(Arrays.asList(str));
    }

    public T appendStrings(List<String> strings) throws IOException {
        return strings(newAppender(), strings);
    }

    private T strings(Writer dest, List<String> strings) throws IOException {
        for (String str : strings) {
            dest.write(str);
        }
        dest.close();
        return (T) this;
    }

    /** @param line without tailing line separator */
    public T writeLines(String... line) throws IOException {
        return writeLines(Arrays.asList(line));
    }

    /** @param lines without tailing line separator */
    public T writeLines(List<String> lines) throws IOException {
        return writeLines(lines.iterator());
    }

    /** @param lines without tailing line separator */
    public T writeLines(Iterator<String> lines) throws IOException {
        return lines(newWriter(), lines);
    }

    /** @param line without tailing line separator */
    public T appendLines(String... line) throws IOException {
        return appendLines(Arrays.asList(line));
    }

    /** @param lines without tailing line separator */
    public T appendLines(List<String> lines) throws IOException {
        return appendLines(lines.iterator());
    }

    /** @param lines without tailing line separator */
    public T appendLines(Iterator<String> lines) throws IOException {
        return lines(newAppender(), lines);
    }

    /** @param lines without tailing line separator */
    private T lines(Writer dest, Iterator<String> lines) throws IOException {
        String line;
        String separator;

        separator = getWorld().getSettings().lineSeparator.getSeparator();
        while (lines.hasNext()) {
            line = lines.next();
            dest.write(line);
            dest.write(separator);
        }
        dest.close();
        return (T) this;
    }

    public T writeProperties(Properties p) throws IOException {
        return writeProperties(p, null);
    }

    public T writeProperties(Properties p, String comment) throws IOException {
        try (Writer dest = newWriter()) {
            p.store(dest, comment);
        }
        return (T) this;
    }

    public T writeObject(Serializable obj) throws IOException {
        try (ObjectOutputStream out = newObjectOutputStream()) {
            out.writeObject(obj);
        }
        return (T) this;
    }

    /** Convenience method for writeXml(node, true); */
    public T writeXml(org.w3c.dom.Node node) throws IOException {
        return (T) writeXml(node, true);
    }

    /**
     * Write the specified node into this file. Adds indentation/newlines when format is true. Otherwise, writes
     * the document "as is" (but always prefixes the document with an xml declaration and encloses attributes in
     * double quotes).
     *
     * @return this node
     */
    public T writeXml(org.w3c.dom.Node node, boolean format) throws IOException {
        getWorld().getXml().getSerializer().serialize(node, this, format);
        return (T) this;
    }

    //-- copy

    /**
     * Copies this to dest. Overwrites existing file and adds to existing directories.
     *
     * @throws NodeNotFoundException if this does not exist
     */
    public void copy(Node dest) throws NodeNotFoundException, CopyException {
        try {
            if (isDirectory()) {
                dest.mkdirOpt();
                copyDirectory(dest);
            } else {
                copyFile(dest);
            }
        } catch (FileNotFoundException | CopyException e) {
            throw e;
        } catch (IOException e) {
            throw new CopyException(this, dest, e);
        }
    }

    public void copyInto(Node directory) throws ExistsException, NodeNotFoundException, CopyException {
        directory.checkDirectory();
        copy(directory.join(getName()));
    }

    /**
     * Overwrites dest if it already exists.
     * @return dest
     */
    public Node copyFile(Node dest) throws FileNotFoundException, CopyException {
        try (OutputStream out = dest.newOutputStream()) {
            copyFileTo(out);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new CopyException(this, dest, e);
        }
        return this;
    }

    /**
     * Convenience method for copy all files. Does not use default-excludes
     * @return list of files and directories created
     */
    public List<Node> copyDirectory(Node dest) throws DirectoryNotFoundException, CopyException {
        return copyDirectory(dest, new Filter().includeAll());
    }

    /**
     * Throws an exception is this or dest is not a directory. Overwrites existing files in dest.
     * @return list of files and directories created
     */
    public List<Node> copyDirectory(Node destdir, Filter filter) throws DirectoryNotFoundException, CopyException {
        return new Copy(this, filter).directory(destdir);
    }

    /** Overwrites this node with content from src. Does not close src. */
    public abstract void copyFileFrom(InputStream src) throws FileNotFoundException, CopyFileFromException;

    /* copyFileFrom implementation with streams */
    public long copyFileFromImpl(InputStream src) throws FileNotFoundException, CopyFileFromException {
        try (OutputStream dest = newOutputStream()) {
            return getWorld().getBuffer().copy(src, dest);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new CopyFileFromException(this, e);
        }
    }

    /**
     * Concenience method for <code>copyFileTo(dest, 0)</code>.
     *
     * @return bytes actually written
     * @throws FileNotFoundException when this node is not a file
     * @throws CopyFileToException for other errors
     */
    public long copyFileTo(OutputStream dest) throws FileNotFoundException, CopyFileToException {
        return copyFileTo(dest, 0);
    }

    /**
     * Writes all bytes except "skip" initial bytes of this node to out. Without closing out afterwards.
     * Writes nothing if this node has less than skip bytes.
     *
     * @return bytes actually written
     * @throws FileNotFoundException when this node is not a file
     * @throws CopyFileToException for other errors
     */
    public abstract long copyFileTo(OutputStream dest, long skip) throws FileNotFoundException, CopyFileToException;

    /* copyFileTo implementation with streams */
    public long copyFileToImpl(OutputStream dest, long skip) throws FileNotFoundException, CopyFileToException {
        long result;

        try (InputStream src = newInputStream()) {
            if (skip(src, skip)) {
                return 0;
            }
            result = getWorld().getBuffer().copy(src, dest);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new CopyFileToException(this, e);
        }
        return result;
    }

    /** @return true when EOF was seen */
    public static boolean skip(InputStream src, long count) throws IOException {
        long step;
        int c;

        while (count > 0) {
            step = src.skip(count);
            if (step == 0) {
                // ByteArrayInputStream just return 0 when at end of file
                c = src.read();
                if (c < 0) {
                    // EOF
                    src.close();
                    return true;
                } else {
                    count--;
                }
            } else {
                count -= step;
            }
        }
        return false;
    }

    //-- move

    /**
     * Convenience Method for move(dest, false).
     */
    public Node move(Node dest) throws NodeNotFoundException, MoveException {
        return move(dest, false);
    }

    /**
     * Moves this file or directory to dest. Throws an exception if this does not exist or if dest already exists.
     * This method is a default implementation with copy and delete, derived classes should override it with a native
     * implementation when available.
     *
     * @param overwrite false reports an error if the target already exists. true can be usued to implement atomic updates.
     * @return dest
     * @throws FileNotFoundException if this does not exist
     */
    public Node move(Node dest, boolean overwrite) throws NodeNotFoundException, MoveException {
        try {
            if (!overwrite) {
                dest.checkNotExists();
            }
            copy(dest);
            deleteTree();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new MoveException(this, dest, "move failed", e);
        }
        return dest;
    }

    public void moveInto(Node directory) throws ExistsException, NodeNotFoundException, MoveException {
        directory.checkDirectory();
        move(directory.join(getName()));
    }

    //-- links

    /**
     * Creates an absolute link dest pointing to this. The signature of this method resembles the copy method.
     *
     * @param dest link to be created
     * @return dest;
     */
    public T link(T dest) throws LinkException {
        if (!getClass().equals(dest.getClass())) {
            throw new IllegalArgumentException(this.getClass() + " vs " + dest.getClass());
        }
        try {
            checkExists();
        } catch (IOException e) {
            throw new LinkException(this, e);
        }
        // TODO: getRoot() for ssh root ...
        dest.mklink(Filesystem.SEPARATOR_STRING + this.getPath());
        return dest;
    }

    /**
     * Creates this link, pointing to the specified target. Throws an exception if this already exists or if the
     * parent does not exist; the target is not checked, it may be absolute or relative
     */
    public abstract void mklink(String target) throws LinkException;

    /**
     * Returns the link target of this file or throws an exception.
     */
    public abstract String readLink() throws ReadLinkException;

    /**
     * Throws an exception if this is not a link.
     */
    public T resolveLink() throws ReadLinkException {
        String path;

        path = readLink();
        if (path.startsWith(Filesystem.SEPARATOR_STRING)) {
            return getRoot().node(path.substring(1), null);
        } else {
            return (T) getParent().join(path);
        }
    }

    //-- diff

    public String diffDirectory(Node rightdir) throws IOException {
        return diffDirectory(rightdir, false);
    }

    public String diffDirectory(Node rightdir, boolean brief) throws IOException {
        return new Diff(brief).directory(this, rightdir, getWorld().filter().includeAll());
    }

    /** cheap diff if you only need a yes/no answer */
    public boolean diff(Node right) throws IOException {
        return diff(right, new Buffer(getWorld().getBuffer()));
    }

    /** cheap diff if you only need a yes/no answer */
    public boolean diff(Node right, Buffer rightBuffer) throws IOException {
        Buffer leftBuffer;
        int leftChunk;
        int rightChunk;
        boolean result;

        leftBuffer = getWorld().getBuffer();
        try (InputStream leftSrc = newInputStream();
             InputStream rightSrc = right.newInputStream()) {
            result = false;
            do {
                leftChunk = leftBuffer.fill(leftSrc);
                rightChunk = rightBuffer.fill(rightSrc);
                if (leftChunk != rightChunk || leftBuffer.diff(rightBuffer, leftChunk)) {
                    result = true;
                    break;
                }
            } while (leftChunk > 0);
        }
        return result;
    }

    //-- search for child nodes

    /** uses default excludes */
    public List<T> find(String... includes) throws IOException {
        return find(getWorld().filter().include(includes));
    }

    public T findOne(String include) throws IOException {
        T found;

        found = findOpt(include);
        if (found == null) {
            throw new FileNotFoundException(this, "nothing matches this pattern: " + include);
        }
        return found;
    }

    public T findOpt(String include) throws IOException {
        List<T> found;

        found = find(include);
        switch (found.size()) {
        case 0:
            return null;
        case 1:
            return found.get(0);
        default:
            throw new IOException(toString() + ": ambiguous: " + include);
        }
    }

    public List<T> find(Filter filter) throws IOException {
        return (List) filter.collect(this);
    }

    //-- other

    public void xslt(Transformer transformer, Node dest) throws IOException, TransformerException {
        try (InputStream in = newInputStream();
             OutputStream out = dest.newOutputStream()) {
            transformer.transform(new StreamSource(in), new StreamResult(out));
        }
    }

    public void gzip(Node dest) throws IOException {
        try (InputStream in = newInputStream();
             OutputStream rawOut = dest.newOutputStream();
             OutputStream out = new GZIPOutputStream(rawOut)) {
            getWorld().getBuffer().copy(in, out);
        }
    }

    public void gunzip(Node dest) throws IOException {
        try (InputStream rawIn = newInputStream();
             InputStream in = new GZIPInputStream(rawIn);
             OutputStream out = dest.newOutputStream()) {
            getWorld().getBuffer().copy(in, out);
        }
    }

    public String sha() throws IOException {
        try {
            return digest("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String md5() throws IOException {
        try {
            return digest("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] digestBytes(String name) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest;
        Buffer buffer;

        try (InputStream src =  newInputStream()) {
            digest = MessageDigest.getInstance(name);
            synchronized (digest) {
                buffer = getWorld().getBuffer();
                synchronized (buffer) {
                    buffer.digest(src, digest);
                }
                return digest.digest();
            }
        }
    }

    public String digest(String name) throws IOException, NoSuchAlgorithmException {
        return Strings.toHex(digestBytes(name));
    }

    //-- Object functionality

    @Override
    public boolean equals(Object obj) {
        Node node;

        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        node = (Node) obj;
        if (!getPath().equals(node.getPath())) {
            return false;
        }
        return getRoot().equals(node.getRoot());
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    /**
     * Returns a String representation suitable for messages.
     *
     * CAUTION: don't use to convert to a string, use instead.
     */
    @Override
    public String toString() {
        Node working;

        working = getWorld().getWorking();
        if (working == null || !getRoot().equals(working.getRoot())) {
            return getUri().toString();
        } else {
            if (hasAncestor(working)) {
                return getRelative(working);
            } else {
                return Filesystem.SEPARATOR_STRING + getPath();
            }
        }
    }

    //--

    /** TODO: is there a better way ... ? */
    public static String encodePath(String path) {
        URI tmp;

        try {
            tmp = new URI("foo", "host", "/" + path, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return tmp.getRawPath().substring(1);
    }

    /** TODO: is there a better way ... ? */
    public static String decodePath(String path) {
        URI tmp;

        try {
            tmp = new URI("scheme://host/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        return tmp.getPath().substring(1);
    }
}
