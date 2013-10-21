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
import net.oneandone.sushi.io.Buffer;
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
 * <p>Your application usually creates some "working-directory" nodes with <code>world.node(URI)</code>.
 * They will be used to create actual working nodes with <code>node.join(path)</code>. The constructor
 * of the respective node class is rarely used directly, it's used indirectly by the filesystem. </p>
 *
 * <p>A node is immutable, except for its base.</p>
 *
 * <p>Method names try to be short, but no abbreviations. Exceptions from this rule are mkfile, mkdir and
 * mklink, because mkdir is a well-established name.</p>
 *
 * <p>If an Implementation cannot (or does not want to) implement a method (e.g. move), it throws an
 * UnsupportedOperationException.</p>
 *
 * <p>You can read nodes using traditional InputStream or Writers. In addition to this pull-logic, the writoTo method
 * provides push logic. Some Node implementations are more efficient when using writeTo(). (I'd appriciate if all
 * underlying libraries provides pull logic because, push logic can be efficiently implemented on top ...) </p>
 *
 * <p>As long as you stick to read operations, nodes are thread-save.</p>
 *
 * <p>Exception handling: throws NodeNotFoundException, FileNotFoundException, DirectoryNotFoundException to indicate
 * a node, file or directory is expected to exist, but it does not.</p>
 */
public abstract class Node {
    protected UnsupportedOperationException unsupported(String op) {
        return new UnsupportedOperationException(getURI() + ":" + op);
    }

    public abstract Root<?> getRoot();

    public Node getRootNode() {
        return getRoot().node("", null);
    }

    public World getWorld() {
        return getRoot().getFilesystem().getWorld();
    }

    /**
     * Creates a stream to read this node.
     * Closing the stream more than once is ok, but reading from a closed stream is rejected by an exception
     */
    public abstract InputStream createInputStream() throws FileNotFoundException, CreateInputStreamException;

    /**
     * Concenience method for <code>writeTo(dest, 0)</code>.
     *
     * @return bytes actually written
     * @throws FileNotFoundException when this node is not a file
     * @throws WriteToException for other errors
     */
    public long writeTo(OutputStream dest) throws FileNotFoundException, WriteToException {
        return writeTo(dest, 0);
    }

    /**
     * Writes all bytes except "skip" initial bytes of this node to out. Without closing out afterwards.
     * Writes nothing if this node has less than skip bytes.
     *
     * @return bytes actually written
     * @throws FileNotFoundException when this node is not a file
     * @throws WriteToException for other errors
     */
    public abstract long writeTo(OutputStream dest, long skip) throws FileNotFoundException, WriteToException;

    public long writeToImpl(OutputStream dest, long skip) throws FileNotFoundException, WriteToException {
        long result;

        try (InputStream src = createInputStream()) {
            if (skip(src, skip)) {
                return 0;
            }
            result = getWorld().getBuffer().copy(src, dest);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new WriteToException(this, e);
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

    public OutputStream createOutputStream() throws FileNotFoundException, CreateOutputStreamException {
        return createOutputStream(false);
    }
    public OutputStream createAppendStream() throws FileNotFoundException, CreateOutputStreamException {
        return createOutputStream(true);
    }

    /**
     * Create a stream to write this node.
     * Closing the stream more than once is ok, but writing to a closed stream is rejected by an exception.
     * @throws FileNotFoundException if this node is a directory
     */
    public abstract OutputStream createOutputStream(boolean append) throws FileNotFoundException, CreateOutputStreamException;

    /**
     * Lists child nodes of this node.
     * @return List of child nodes or null if this node is a file. Note that returning null allows for optimizations
     *    because list() may be called on any existing node; otherwise, you'd have to inspect the resulting exception
     *    whether you called list on a file.
     * @throws ListException if this does not exist (in this case, cause is set to a FileNotFoundException),
     *    permission is denied, or another IO problem occurs.
     */
    public abstract List<? extends Node> list() throws ListException, DirectoryNotFoundException;

    /**
     * Fails if the directory already exists. Features define whether is operation is atomic.
     * @return this
     */
    public abstract Node mkdir() throws MkdirException;

    /**
     * Fails if the directory already exists. Features define whether this operation is atomic.
     * This default implementation is not atomic.
     * @return this
     */
    public Node mkfile() throws MkfileException {
    	try {
			if (exists()) {
				throw new MkfileException(this);
			}
			writeBytes();
		} catch (IOException e) {
			throw new MkfileException(this, e);
		}
		return this;
    }


    /**
     * Deletes this node, no matter if it's a file or a directory. If this is a link, the link is deleted, not the link target.
     *
     * @return this
     */
    public abstract Node deleteTree() throws NodeNotFoundException, DeleteException;

    /** @throws DeleteException if this is not file */
    public abstract Node deleteFile() throws FileNotFoundException, DeleteException;

    /** @throws DeleteException if this is not a directory or the directory is not empty */
    public abstract Node deleteDirectory() throws DirectoryNotFoundException, DeleteException;

    /**
     * Convenience Method for move(dest, false).
     */
    public Node move(Node dest) throws MoveException {
        return move(dest, false);
    }

    /**
     * Moves this file or directory to dest. Throws an exception if this does not exist or if dest already exists.
     * This method is a default implementation with copy and delete, derived classes should override it with a native
     * implementation when available.
     *
     * @param overwrite false reports an error if the target already exists. true can be usued to implement atomic updates.
     * @return dest
     */
    public Node move(Node dest, boolean overwrite) throws MoveException {
        try {
            if (!overwrite) {
                dest.checkNotExists();
            }
            copy(dest);
            deleteTree();
        } catch (IOException e) {
            throw new MoveException(this, dest, "move failed", e);
        }
        return dest;
    }

    //-- status methods

    /** Throws a LengthException if this node is not a file. */
    public abstract long length() throws LengthException;

    /**
     * Tests if this is a file, directory or link.
     * @return true if the file exists, even if it's a dangling link.
     */
    public abstract boolean exists() throws ExistsException;

    public abstract boolean isFile() throws ExistsException;
    public abstract boolean isDirectory() throws ExistsException;
    public abstract boolean isLink() throws ExistsException;

    /** Throws an exception is the file does not exist */
    public abstract long getLastModified() throws GetLastModifiedException;
    public abstract void setLastModified(long millis) throws SetLastModifiedException;

    public abstract String getPermissions() throws ModeException;
    public abstract void setPermissions(String permissions) throws ModeException;

    public abstract UserPrincipal getOwner() throws ModeException;
    public abstract void setOwner(UserPrincipal owner) throws ModeException;
    public abstract GroupPrincipal getGroup() throws ModeException;
    public abstract void setGroup(GroupPrincipal group) throws ModeException;

    //-- path functionality

    /**
     * Never starts or end with a slash or a drive; an empty string is the root path. The path is decoded,
     * you have to encoded if you want to build an URI.
     */
    public abstract String getPath();

    /** @return a normalized URI, not necessarily the URI this node was created from */
    public URI getURI() {
        return URI.create(getRoot().getFilesystem().getScheme() + ":" + getRoot().getId() + encodePath(getPath()));
    }


    /** @return the last path segment (or an empty string for the root node */
    public String getName() {
        String path;

        path = getPath();
        // ok for -1:
        return path.substring(path.lastIndexOf(Filesystem.SEPARATOR_CHAR) + 1);
    }

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


    public abstract Node getParent();
    protected Node doGetParent() {
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

    public boolean hasDifferentAnchestor(Node anchestor) {
        Node parent;

        parent = getParent();
        if (parent == null) {
            return false;
        } else {
            return parent.hasAnchestor(anchestor);
        }
    }

    public boolean hasAnchestor(Node anchestor) {
        Node current;

        current = this;
        while (true) {
            if (current.equals(anchestor)) {
                return true;
            }
            current = current.getParent();
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

    public abstract Node join(List<String> paths);

    protected Node doJoin(List<String> paths) {
        Root<?> root;
        Node result;

        root = getRoot();
        result = root.node(root.getFilesystem().join(getPath(), paths), null);
        return result;
    }

    public abstract Node join(String... names);

    public Node doJoin(String... names) {
        return join(Arrays.asList(names));
    }

    //-- input stream functionality

    public NodeReader createReader() throws IOException {
        return NodeReader.create(this);
    }

    public ObjectInputStream createObjectInputStream() throws IOException {
        return new ObjectInputStream(createInputStream());
    }

    /**
     * Reads all bytes of the node.
     *
     * Default implementation that works for all nodes: reads the file in chunks and builds the result in memory.
     * Derived classes should override it if they can provide a more efficient implementation, e.g. by determining
     * the length first if getting the length is cheap.
     *
     * @throws IOException
     */
    public byte[] readBytes() throws IOException {
        Buffer buffer;

        try (InputStream src = createInputStream()) {
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
        return new LineReader(createReader(), format).collect();
    }

    /** Reads properties with the encoding for this node */
    public Properties readProperties() throws IOException {
        Properties p;

        try (Reader src = createReader()) {
            p = new Properties();
            p.load(src);
        }
        return p;
    }

    public Object readObject() throws IOException {
        Object result;

        try (ObjectInputStream src = createObjectInputStream()) {
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

        try (InputStream in = createInputStream()) {
            templates = Serializer.templates(new SAXSource(new InputSource(in)));
        }
        return templates.newTransformer();
    }

    public void xslt(Transformer transformer, Node dest) throws IOException, TransformerException {
        try (InputStream in = createInputStream();
             OutputStream out = dest.createOutputStream()) {
            transformer.transform(new StreamSource(in), new StreamResult(out));
        }
    }

    //--

    public Node checkExists() throws ExistsException, NodeNotFoundException {
        if (!exists()) {
            throw new NodeNotFoundException(this);
        }
        return this;
    }

    public Node checkNotExists() throws ExistsException, NodeAlreadyExistsException {
        if (exists()) {
            throw new NodeAlreadyExistsException(this);
        }
        return this;
    }

    public Node checkDirectory() throws ExistsException, DirectoryNotFoundException {
        if (isDirectory()) {
            return this;
        }
        if (exists()) {
            throw new DirectoryNotFoundException(this, "directory not found - this is a file");
        } else {
            throw new DirectoryNotFoundException(this);
        }
    }

    public Node checkFile() throws ExistsException, FileNotFoundException {
        if (isFile()) {
            return this;
        }
        if (exists()) {
            throw new FileNotFoundException(this, "file not found - this is a directory");
        } else {
            throw new FileNotFoundException(this);
        }
    }

    //--

    /**
     * Creates an absolute link. The signature of this method resembles the copy method.
     *
     * @param dest symlink to be created
     * @return dest;
     */
    public Node link(Node dest) throws LinkException {
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
     * Creates this link, pointing to the specified path. Throws an exception if this already exists or if the
     * parent does not exist; the target is not checked, it may be absolute or relative
     */
    public abstract void mklink(String path) throws LinkException;

    /**
     * Returns the link target of this file or throws an exception.
     */
    public abstract String readLink() throws ReadLinkException;

    /**
     * Throws an exception if this is not a link.
     */
    public Node resolveLink() throws ReadLinkException {
        String path;

        path = readLink();
        if (path.startsWith(Filesystem.SEPARATOR_STRING)) {
            return getRoot().node(path.substring(1), null);
        } else {
            return getParent().join(path);
        }
    }

    public void copy(Node dest) throws CopyException {
        try {
            if (isDirectory()) {
                dest.mkdirOpt();
                copyDirectory(dest);
            } else {
                copyFile(dest);
            }
        } catch (CopyException e) {
            throw e;
        } catch (IOException e) {
            throw new CopyException(this, dest, e);
        }
    }

    /**
     * Overwrites dest if it already exists.
     * @return dest
     */
    public Node copyFile(Node dest) throws CopyException {
        try (InputStream in = createInputStream()) {
            getWorld().getBuffer().copy(in, dest);
            return dest;
        } catch (IOException e) {
            throw new CopyException(this, dest, e);
        }
    }

    /**
     * Convenience method for copy all files. Does not use default-excludes
     * @return list of files and directories created
     */
    public List<Node> copyDirectory(Node dest) throws CopyException {
        return copyDirectory(dest, new Filter().includeAll());
    }

    /**
     * Throws an exception is this or dest is not a directory. Overwrites existing files in dest.
     * @return list of files and directories created
     */
    public List<Node> copyDirectory(Node destdir, Filter filter) throws CopyException {
        return new Copy(this, filter).directory(destdir);
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
        boolean[] leftEof;
        boolean[] rightEof;
        boolean result;

        leftBuffer = getWorld().getBuffer();
        try (InputStream leftSrc = createInputStream();
             InputStream rightSrc = right.createInputStream()) {
            leftEof = new boolean[] { false };
            rightEof = new boolean[] { false };
            result = false;
            do {
                leftChunk = leftEof[0] ? 0 : leftBuffer.fill(leftSrc, leftEof);
                rightChunk = rightEof[0] ? 0 : rightBuffer.fill(rightSrc, rightEof);
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
    public List<Node> find(String... includes) throws IOException {
        return find(getWorld().filter().include(includes));
    }

    public Node findOne(String include) throws IOException {
        Node found;

        found = findOpt(include);
        if (found == null) {
            throw new FileNotFoundException(this, "nothing matches this pattern: " + include);
        }
        return found;
    }

    public Node findOpt(String include) throws IOException {
        List<Node> found;

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

    public List<Node> find(Filter filter) throws IOException {
        return filter.collect(this);
    }

    //--

    public Node deleteFileOpt() throws IOException {
        if (exists()) {
            deleteFile();
        }
        return this;
    }

    public Node deleteDirectoryOpt() throws IOException {
        if (exists()) {
            deleteDirectory();
        }
        return this;
    }

    public Node deleteTreeOpt() throws IOException {
        if (exists()) {
            deleteTree();
        }
        return this;
    }

    public Node mkdirOpt() throws MkdirException {
        try {
			if (!isDirectory()) {
			    mkdir(); // fail here if it's a file!
			}
		} catch (ExistsException e) {
			throw new MkdirException(this, e);
		}
        return this;
    }

    public Node mkdirsOpt() throws MkdirException {
        Node parent;

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
        return this;
    }

    public Node mkdirs() throws MkdirException {
    	try {
    		if (exists()) {
    			throw new MkdirException(this);
    		}
    	    return mkdirsOpt();
    	} catch (IOException e) {
    		throw new MkdirException(this, e);
    	}
    }

    //-- output create functionality

    public NodeWriter createWriter() throws IOException {
        return createWriter(false);
    }

    public NodeWriter createAppender() throws IOException {
        return createWriter(true);
    }

    public NodeWriter createWriter(boolean append) throws IOException {
        return NodeWriter.create(this, append);
    }

    public ObjectOutputStream createObjectOutputStream() throws IOException {
        return new ObjectOutputStream(createOutputStream());
    }

    public Node writeBytes(byte ... bytes) throws IOException {
        return writeBytes(bytes, 0, bytes.length, false);
    }

    public Node appendBytes(byte ... bytes) throws IOException {
        return writeBytes(bytes, 0, bytes.length, true);
    }

    public Node writeBytes(byte[] bytes, int ofs, int len, boolean append) throws IOException {
        try (OutputStream out = createOutputStream(append)) {
            out.write(bytes, ofs, len);
        }
        return this;
    }

    public Node writeChars(char ... chars) throws IOException {
        return writeChars(chars, 0, chars.length, false);
    }

    public Node appendChars(char ... chars) throws IOException {
        return writeChars(chars, 0, chars.length, true);
    }

    public Node writeChars(char[] chars, int ofs, int len, boolean append) throws IOException {
        try (Writer out = createWriter(append)) {
            out.write(chars, ofs, len);
        }
        return this;
    }

    public Node writeString(String txt) throws IOException {
        try (Writer w = createWriter()) {
            w.write(txt);
        }
        return this;
    }

    public Node appendString(String txt) throws IOException {
        try (Writer w = createAppender()) {
            w.write(txt);
        }
        return this;
    }

    public Node writeStrings(String ... str) throws IOException {
        return writeStrings(Arrays.asList(str));
    }

    public Node writeStrings(List<String> strings) throws IOException {
        return strings(createWriter(), strings);
    }

    public Node appendStrings(String ... str) throws IOException {
        return appendStrings(Arrays.asList(str));
    }

    public Node appendStrings(List<String> strings) throws IOException {
        return strings(createAppender(), strings);
    }

    private Node strings(Writer dest, List<String> strings) throws IOException {
        for (String str : strings) {
            dest.write(str);
        }
        dest.close();
        return this;
    }

    /** @param line without tailing line separator */
    public Node writeLines(String ... line) throws IOException {
        return writeLines(Arrays.asList(line));
    }

    /** @param lines without tailing line separator */
    public Node writeLines(List<String> lines) throws IOException {
        return lines(createWriter(), lines);
    }

    /** @param line without tailing line separator */
    public Node appendLines(String ... line) throws IOException {
        return appendLines(Arrays.asList(line));
    }

    /** @param lines without tailing line separator */
    public Node appendLines(List<String> lines) throws IOException {
        return lines(createAppender(), lines);
    }

    /** @param lines without tailing line separator */
    private Node lines(Writer dest, List<String> lines) throws IOException {
        String separator;

        separator = getWorld().getSettings().lineSeparator.getSeparator();
        for (String line : lines) {
            dest.write(line);
            dest.write(separator);
        }
        dest.close();
        return this;
    }

    public Node writeProperties(Properties p) throws IOException {
        return writeProperties(p, null);
    }

    public Node writeProperties(Properties p, String comment) throws IOException {
        try (Writer dest = createWriter()) {
            p.store(dest, comment);
        }
        return this;
    }

    public Node writeObject(Serializable obj) throws IOException {
        try (ObjectOutputStream out = createObjectOutputStream()) {
            out.writeObject(obj);
        }
        return this;
    }

    /** Convenience method for writeXml(node, true); */
    public Node writeXml(org.w3c.dom.Node node) throws IOException {
        return writeXml(node, true);
    }

    /**
     * Write the specified node into this file. Adds indentation/newlines when format is true. Otherwise, writes
     * the document "as is" (but always prefixes the document with an xml declaration and encloses attributes in
     * double quotes).
     *
     * @return this node
     */
    public Node writeXml(org.w3c.dom.Node node, boolean format) throws IOException {
        getWorld().getXml().getSerializer().serialize(node, this, format);
        return this;
    }

    //-- other

    public void gzip(Node dest) throws IOException {
        try (InputStream in = createInputStream();
             OutputStream rawOut = dest.createOutputStream();
             OutputStream out = new GZIPOutputStream(rawOut)) {
            getWorld().getBuffer().copy(in, out);
        }
    }

    public void gunzip(Node dest) throws IOException {
        try (InputStream rawIn = createInputStream();
             InputStream in = new GZIPInputStream(rawIn);
             OutputStream out = dest.createOutputStream()) {
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

        try (InputStream src =  createInputStream()) {
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
            return getURI().toString();
        } else {
            if (hasAnchestor(working)) {
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
