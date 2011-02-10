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

import net.sf.beezle.sushi.archive.Archive;
import net.sf.beezle.sushi.fs.filter.Filter;
import net.sf.beezle.sushi.fs.zip.ZipFilesystem;
import net.sf.beezle.sushi.fs.zip.ZipNode;
import net.sf.beezle.sushi.io.Buffer;
import net.sf.beezle.sushi.util.Strings;
import net.sf.beezle.sushi.xml.Serializer;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
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
 * <p>The path is a sequence of names separated by the filesystem separator. It never starts
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
 */
public abstract class Node {
    protected UnsupportedOperationException unsupported(String op) {
        return new UnsupportedOperationException(getURI() + ":" + op);
    }

    public abstract Root getRoot();

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
    public abstract InputStream createInputStream() throws IOException;

    public OutputStream createOutputStream() throws IOException {
        return createOutputStream(false);
    }
    public OutputStream createAppendStream() throws IOException {
        return createOutputStream(true);
    }

    /**
     * Create a stream to write this node.
     * Closing the stream more than once is ok, but writing to a closed stream is rejected by an exception.
     */
    public abstract OutputStream createOutputStream(boolean append) throws IOException;

    /**
     * Lists child nodes of this node.
     * @return List of child nodes or null if this node is a file. Note that returning null allows for optimizations
     *    because list() may be called on any existing node; otherwise, you'd have to inspect the resulting exception
     *    whether you called list on a file.
     * @throws ListException if this does not exist or permission is denied.
     */
    public abstract List<? extends Node> list() throws ListException;

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
    public abstract Node delete() throws DeleteException;

    /**
     * Moves this file or directory to dest. Throws an exception if this does not exist or if dest already exists.
     * This method is a default implementation with copy and delete, derived classes should override it with a native
     * implementation when available.
     *
     * @return dest
     */
    public Node move(Node dest) throws MoveException {
        try {
            dest.checkNotExists();
            copy(dest);
            delete();
        } catch (IOException e) {
            throw new MoveException(this, dest, "move failed", e);
        }
        return dest;
    }

    //-- status methods

    /** Throws an Exception if this node is not a file. */
    public abstract long length() throws LengthException;

    /** @return true if the file exists, even if it's a dangling link */
    public abstract boolean exists() throws ExistsException;

    public abstract boolean isFile() throws ExistsException;
    public abstract boolean isDirectory() throws ExistsException;
    public abstract boolean isLink() throws ExistsException;

    /** Throws an exception is the file does not exist */
    public abstract long getLastModified() throws GetLastModifiedException;
    public abstract void setLastModified(long millis) throws SetLastModifiedException;

    public abstract int getMode() throws IOException;
    public abstract void setMode(int mode) throws IOException;

    public abstract int getUid() throws IOException;
    public abstract void setUid(int id) throws IOException;
    public abstract int getGid() throws IOException;
    public abstract void setGid(int id) throws IOException;

    //-- path functionality

    public abstract String getPath();

    /** @return a normalized URI, not necesarily the URI this node was created from */
    public URI getURI() {
        return URI.create(getRoot().getFilesystem().getScheme() + ":" + getRoot().getId() + getPath());
    }

    /** @return the last path segment (or an empty string for the root node */
    public String getName() {
        String path;

        path = getPath();
        // ok for -1:
        return path.substring(path.lastIndexOf(getRoot().getFilesystem().getSeparatorChar()) + 1);
    }

    public abstract Node getParent();
    protected Node doGetParent() {
        String path;
        int idx;

        path = getPath();
        if ("".equals(path)) {
            return null;
        }
        idx = path.lastIndexOf(getRoot().getFilesystem().getSeparatorChar());
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
        Filesystem fs;

        if (base.equals(this)) {
            return ".";
        }
        fs = getRoot().getFilesystem();
        startfilepath = base.join("foo").getPath();
        destpath = getPath();
        common = Strings.getCommon(startfilepath, destpath);
        common = common.substring(0, common.lastIndexOf(fs.getSeparatorChar()) + 1);  // ok for idx == -1
        len = common.length();
        startfilepath = startfilepath.substring(len);
        destpath = destpath.substring(len);
        result = new StringBuilder();
        ups = Strings.count(startfilepath, fs.getSeparator());
        for (i = 0; i < ups; i++) {
            result.append("..").append(fs.getSeparator());
        }
        result.append(Strings.replace(destpath, getWorld().os.lineSeparator, "" + getWorld().os.lineSeparator));
        return result.toString();
    }

    public abstract Node join(List<String> paths);

    protected Node doJoin(List<String> paths) {
        Root root;
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
     * @return
     * @throws IOException
     */
    public byte[] readBytes() throws IOException {
        InputStream src;
        byte[] result;

        src = createInputStream();
        result = getWorld().getBuffer().readBytes(src);
        src.close();
        return result;
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

    public Object readObject() throws IOException {
        ObjectInputStream src;
        Object result;

        src = createObjectInputStream();
        try {
            result = src.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        src.close();
        return result;
    }

    public Document readXml() throws IOException, SAXException {
        return getWorld().getXml().builder.parse(this);
    }

    public Transformer readXsl() throws IOException, TransformerConfigurationException {
        InputStream in;
        Templates templates;

        in = createInputStream();
        templates = Serializer.templates(new SAXSource(new InputSource(in)));
        in.close();
        return templates.newTransformer();
    }

    public void xslt(Transformer transformer, Node dest) throws IOException, TransformerException {
        InputStream in;
        OutputStream out;

        in = createInputStream();
        out = dest.createOutputStream();
        transformer.transform(new StreamSource(in), new StreamResult(out));
        out.close();
        in.close();
    }

    //--

    public Node checkExists() throws IOException {
        if (!exists()) {
            throw new IOException("no such file or directory: " + this);
        }
        return this;
    }

    public Node checkNotExists() throws IOException {
        if (exists()) {
            throw new IOException("file or directory already exists: " + this);
        }
        return this;
    }

    public Node checkDirectory() throws ExistsException, FileNotFoundException {
        if (isDirectory()) {
            return this;
        }
        if (exists()) {
            throw new FileNotFoundException("directory expected: " + this);
        } else {
            throw new FileNotFoundException("no such directory: " + this);
        }
    }

    public Node checkFile() throws ExistsException, FileNotFoundException {
        if (isFile()) {
            return this;
        }
        if (exists()) {
            throw new FileNotFoundException("file expected: " + this);
        } else {
            throw new FileNotFoundException("no such file: " + this);
        }
    }

    //--

    /**
     * Creates an absolute link. The signature of this method resembles the copy method.
     *
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
        dest.mklink(getRoot().getFilesystem().getSeparator() + this.getPath());
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
        String separator;

        path = readLink();
        separator = getRoot().getFilesystem().getSeparator();
        if (path.startsWith(separator)) {
            return getRoot().node(path.substring(separator.length()), null);
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
        InputStream in;

        try {
            in = createInputStream();
            getWorld().getBuffer().copy(in, dest);
            in.close();
            return dest;
        } catch (IOException e) {
            throw new CopyException(this, dest, e);
        }
    }

    /**
     * Convenience method for copy with filters below.
     * @return list of files and directories created
     */
    public List<Node> copyDirectory(Node dest) throws CopyException {
        return copyDirectory(dest, getWorld().filter().includeAll());
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
        InputStream leftSrc;
        InputStream rightSrc;
        Buffer leftBuffer;
        int leftChunk;
        int rightChunk;
        boolean[] leftEof;
        boolean[] rightEof;
        boolean result;

        leftBuffer = getWorld().getBuffer();
        leftSrc = createInputStream();
        leftEof = new boolean[] { false };
        rightSrc = right.createInputStream();
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
    	leftSrc.close();
    	rightSrc.close();
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
            throw new FileNotFoundException(toString() + ": not found: " + include);
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

    public Node deleteOpt() throws IOException {
        if (exists()) {
            delete();
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
        OutputStream out;

        out = createOutputStream(append);
        out.write(bytes, ofs, len);
        out.close();
        return this;
    }

    public Node writeChars(char ... chars) throws IOException {
        return writeChars(chars, 0, chars.length, false);
    }

    public Node appendChars(char ... chars) throws IOException {
        return writeChars(chars, 0, chars.length, true);
    }

    public Node writeChars(char[] chars, int ofs, int len, boolean append) throws IOException {
        Writer out;

        out = createWriter(append);
        out.write(chars, ofs, len);
        out.close();
        return this;
    }

    public Node writeString(String txt) throws IOException {
        Writer w;

        w = createWriter();
        w.write(txt);
        w.close();
        return this;
    }

    public Node appendString(String txt) throws IOException {
        Writer w;

        w = createAppender();
        w.write(txt);
        w.close();
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

        separator = getWorld().getSettings().lineSeparator;
        for (String line : lines) {
            dest.write(line);
            dest.write(separator);
        }
        dest.close();
        return this;
    }

    public Node writeObject(Serializable obj) throws IOException {
        ObjectOutputStream out;

        out = createObjectOutputStream();
        out.writeObject(obj);
        out.close();
        return this;
    }

    public Node writeXml(org.w3c.dom.Node node) throws IOException {
        getWorld().getXml().serializer.serialize(node, this);
        return this;
    }

    //-- other

    public void gzip(Node dest) throws IOException {
        InputStream in;
        OutputStream out;

        in = createInputStream();
        out = new GZIPOutputStream(dest.createOutputStream());
        getWorld().getBuffer().copy(in, out);
        in.close();
        out.close();
    }

    public void gunzip(Node dest) throws IOException {
        InputStream in;
        OutputStream out;

        in = new GZIPInputStream(createInputStream());
        out = dest.createOutputStream();
        getWorld().getBuffer().copy(in, out);
        in.close();
        out.close();
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
        InputStream src;
        MessageDigest complete;

        src =  createInputStream();
        complete = MessageDigest.getInstance(name);
        getWorld().getBuffer().digest(src, complete);
        src.close();
        return complete.digest();
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
     * CAUTION: don't use to convert to a string, use getRelative and getAbsolute() instead.
     * Also call the respective getter if the difference matters for your representation.
     */
    @Override
    public final String toString() {
        Node working;

        working = getWorld().getWorking();
        if (working == null || !getRoot().equals(working.getRoot())) {
            return getURI().toString();
        } else {
            if (hasAnchestor(working)) {
                return getRelative(working);
            } else {
                return getRoot().getFilesystem().getSeparator() + getPath();
            }
        }
    }
}
