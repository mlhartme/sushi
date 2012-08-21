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

package net.sf.beezle.sushi.fs.file;

import net.sf.beezle.sushi.fs.DeleteException;
import net.sf.beezle.sushi.fs.ExistsException;
import net.sf.beezle.sushi.fs.Filesystem;
import net.sf.beezle.sushi.fs.GetLastModifiedException;
import net.sf.beezle.sushi.fs.LengthException;
import net.sf.beezle.sushi.fs.LinkException;
import net.sf.beezle.sushi.fs.ListException;
import net.sf.beezle.sushi.fs.MkdirException;
import net.sf.beezle.sushi.fs.MkfileException;
import net.sf.beezle.sushi.fs.MoveException;
import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.OnShutdown;
import net.sf.beezle.sushi.fs.ReadLinkException;
import net.sf.beezle.sushi.fs.SetLastModifiedException;
import net.sf.beezle.sushi.fs.WriteToException;
import net.sf.beezle.sushi.fs.zip.ZipFilesystem;
import net.sf.beezle.sushi.fs.zip.ZipNode;
import net.sf.beezle.sushi.io.Buffer;
import net.sf.beezle.sushi.io.OS;
import net.sf.beezle.sushi.launcher.Launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>File, directory, symlink or something not yet created. Replacement for java.world.File.</p>
 */
public class FileNode extends Node {
    private final FileRoot root;

    /** never null, always absolute, never ends with a slash */
    private final Path path;

    public FileNode(FileRoot root, Path path) {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException(path.toString());
        }
        if (path.toString().endsWith(File.separator) && path.getNameCount() > 0) {
            throw new IllegalArgumentException(path.toString());
        }
        this.root = root;
        this.path = path;
    }

    @Override
    public FileRoot getRoot() {
        return root;
    }

    @Override
    public FileNode getParent() {
        return (FileNode) doGetParent();
    }

    @Override
    public FileNode join(String ... paths) {
        return (FileNode) doJoin(paths);
    }

    @Override
    public FileNode join(List<String> paths) {
        return (FileNode) doJoin(paths);
    }

    @Override
    public URI getURI() {
        return path.toFile().toURI();
    }

    /**
     * Avoid calling this method in your code. Should only be used to interact with libs that do not
     * Sushi (File) Nodes.
     */
    public Path toPath() {
        return path;
    }

    /** does not include the drive on windows */
    @Override
    public String getPath() {
    	String result;

    	result = path.toFile().getPath().substring(getRoot().getAbsolute().length());
    	return result.replace(File.separatorChar, Filesystem.SEPARATOR_CHAR);
    }

    public String getAbsolute() {
        return path.toFile().getAbsolutePath();
    }

    //--

    @Override
    public boolean exists() {
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public boolean isFile() {
        return Files.isRegularFile(path);
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    public boolean canWrite() {
        return Files.isReadable(path);
    }

    public boolean canRead() {
        return Files.isWritable(path);
    }

    //--

    public ZipNode openZip() throws IOException {
        return ((ZipFilesystem) getWorld().getFilesystem("zip")).node(this, "");
    }

    public ZipNode openJar() throws IOException {
        return ((ZipFilesystem) getWorld().getFilesystem("jar")).node(this, "");
    }

    /** @return dest */
    public Node unzip(Node dest) throws IOException {
        openZip().copyDirectory(dest);
        return this;
    }

    public Node unjar(Node dest) throws IOException {
        openJar().copyDirectory(dest);
        return this;
    }


    @Override
    public long length() throws LengthException {
        if (!Files.isRegularFile(path)) {
            throw new LengthException(this, new IOException("file expected"));
        }
        return path.toFile().length();
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        long result;

        result = path.toFile().lastModified();
        if (result == 0 && !exists()) {
            throw new GetLastModifiedException(this, new ExistsException(this, null));
        }
        return result;
    }

    @Override
    public void setLastModified(long time) throws SetLastModifiedException {
        if (!path.toFile().setLastModified(time)) {
            throw new SetLastModifiedException(this);
        }
    }

    //-- locating


    /** @return null when called for a file; non-null otherwise */
    @Override
    public List<FileNode> list() throws ListException {
        File[] children;
        List<FileNode> result;

        children = path.toFile().listFiles();
        if (children == null) {
            if (!exists()) {
                throw new ListException(this, new FileNotFoundException(getPath()));
            }
            if (!canRead()) {
                try {
                    if (isLink()) {
                        // TODO: check link target
                        throw new ListException(this, new IOException("broken link"));
                    }
                } catch (IOException e) {
                    // fall through
                }
                throw new ListException(this, new IOException("permission denied"));
            } else {
                return null;
            }
        }
        result = new ArrayList<>(children.length);
        for (File child : children) {
            result.add(new FileNode(root, child.toPath()));
        }
        return result;
    }

    //-- read and writeBytes

    @Override
    public FileInputStream createInputStream() throws IOException {
        return new FileInputStream(path.toFile());
    }

    public long writeTo(OutputStream dest, long skip) throws WriteToException, FileNotFoundException {
        return writeToImpl(dest, skip);
    }

    @Override
    public FileOutputStream createOutputStream(boolean append) throws IOException {
        return new FileOutputStream(path.toFile(), append);
    }

    //-- create

    /** calls createNewFile */
    @Override
    public FileNode mkfile() throws MkfileException {
    	try {
			if (!path.toFile().createNewFile()) {
			    throw new MkfileException(this);
			}
		} catch (IOException e) {
			throw new MkfileException(this, e);
		}
        return this;
    }

    @Override
    public FileNode mkdir() throws MkdirException {
        if (!path.toFile().mkdir()) {
            throw new MkdirException(this);
        }
        return this;
    }

    @Override
    public void mklink(String target) throws LinkException {
        FileNode parent;

        try {
            checkNotExists();
            parent = getParent();
            parent.checkDirectory();
            new Launcher(parent, "ln", "-s", target, getName()).execNoOutput();
        } catch (IOException e) {
            throw new LinkException(this, e);
        }
    }

    @Override
    public String readLink() throws ReadLinkException {
    	try {
		    return getWorld().getTemp().exec("readlink", path.toFile().getAbsolutePath()).trim();
		} catch (IOException e) {
			throw new ReadLinkException(this, e);
		}
    }

    @Override
    public boolean isLink() throws ExistsException {
        return Files.isSymbolicLink(path);
    }

    //-- move

    /** @return dest */
    @Override
    public FileNode move(Node destNode) throws MoveException {
    	FileNode dest;
        Launcher p;
        String output;

        if (!(destNode instanceof FileNode)) {
        	throw new MoveException(this, destNode, "cannot move to none-file-node");
        }
        dest = (FileNode) destNode;
      	try {
      		dest.checkNotExists();
      	} catch (IOException e) {
      		throw new MoveException(this, dest, "dest exists", e);
      	}
        if (getWorld().os == OS.WINDOWS) {
            p = new Launcher(dest.getParent(), "cmd", "/C", "move");
        } else {
            p = new Launcher(dest.getParent(), "mv");
        }
        p.arg(getAbsolute(), dest.getName());
        try {
			output = p.exec();
		} catch (IOException e) {
			throw new MoveException(this, dest, "os command failed", e);
		}
        if (output.length() > 0 && getWorld().os != OS.WINDOWS) {
            throw new MoveException(this, dest, "unexpected output: " + output);
        }
        return dest;
    }

    //-- rename

    public void rename(FileNode target) throws IOException {
        if (target.exists()) {
            throw new IOException("target exists: " + target);
        }
        rename(path.toFile(), target.path.toFile());
    }

    private static void rename(File src, File target) throws IOException {
        if (!src.exists()) {
            throw new FileNotFoundException("" + src);
        }
        // the target may exist, it will be overwritten!
        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory()) {
            throw new IOException("not a directory: " + parent);
        }
        if (!src.renameTo(target)) {
            throw new IOException("Failed to rename " + src + " to " + target);
        }
    }

    //-- delete

    public FileNode deleteFile() throws DeleteException {
        try {
            checkFile();
            Files.delete(path);
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    public FileNode deleteDirectory() throws DeleteException {
        try {
            checkDirectory();
            Files.delete(path);
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    /**
     * Deletes a file or directory. Directories are deleted recursively. Handles Links.
     *
     * @throws IOException if a file cannot be deleted
     */
    @Override
    public FileNode deleteTree() throws DeleteException {
        try {
            doDeleteTree(path.toFile());
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    protected static void doDeleteTree(File file) throws IOException {
        File[] files;

        if (!Files.isSymbolicLink(file.toPath())) {
            files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    doDeleteTree(child);
                }
            }
        }
        Files.delete(file.toPath());
    }

    //--

    @Override
    public boolean diff(Node right, Buffer rightBuffer) throws IOException {
        if (right instanceof FileNode) {
            if (length() != right.length()) {
                return true;
            }
        }
        return super.diff(right, rightBuffer);
    }

    //--

    /** Executes the specified program in this directory. Convenience Method. Don't forget to check the output. */
    public String exec(String ... args) throws IOException {
        return new Launcher(this, args).exec();
    }

    public void execNoOutput(String ... args) throws IOException {
        new Launcher(this, args).execNoOutput();
    }

    //--

    @Override
    public int getMode() throws IOException {
        return stat(OS.CURRENT.mode, 8) & 0777;
    }

    @Override
    public void setMode(int mode) throws IOException {
        ch("chmod", Integer.toOctalString(mode));
    }

    @Override
    public int getUid() throws IOException {
        return stat(OS.CURRENT.uid, 10);
    }

    @Override
    public void setUid(int uid) throws IOException {
        ch("chown", Integer.toString(uid));
    }

    @Override
    public int getGid() throws IOException {
        return stat(OS.CURRENT.gid, 10);
    }

    @Override
    public void setGid(int gid) throws IOException {
        ch("chgrp", Integer.toString(gid));
    }

    private void ch(String cmd, String n) throws IOException {
        new Launcher(getParent(), cmd, n, getAbsolute()).execNoOutput();
    }

    private int stat(String[] args, int radix) throws IOException {
        Launcher stat;

        stat = new Launcher(getParent(), "stat");
        stat.arg(args);
        stat.arg(getAbsolute());
        return Integer.parseInt(stat.exec().trim(), radix);
    }

    //--

    @Override
    public String toString() {
        Node working;

        working = getWorld().getWorking();
        if (working != null && hasAnchestor(working)) {
            return getRelative(working).replace(Filesystem.SEPARATOR_CHAR, File.separatorChar);
        } else {
            return path.toFile().toString();
        }
    }

    //--

    public FileNode createTempFile() throws IOException {
        return OnShutdown.get().createFile(this);
    }

    public FileNode createTempDirectory() throws IOException {
        return OnShutdown.get().createDirectory(this);
    }
}

