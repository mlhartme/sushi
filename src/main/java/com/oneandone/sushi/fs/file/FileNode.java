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

package com.oneandone.sushi.fs.file;

import com.oneandone.sushi.fs.DeleteException;
import com.oneandone.sushi.fs.ExistsException;
import com.oneandone.sushi.fs.GetLastModifiedException;
import com.oneandone.sushi.fs.LengthException;
import com.oneandone.sushi.fs.LinkException;
import com.oneandone.sushi.fs.ListException;
import com.oneandone.sushi.fs.MkdirException;
import com.oneandone.sushi.fs.MkfileException;
import com.oneandone.sushi.fs.MoveException;
import com.oneandone.sushi.fs.Node;
import com.oneandone.sushi.fs.OnShutdown;
import com.oneandone.sushi.fs.ReadLinkException;
import com.oneandone.sushi.fs.SetLastModifiedException;
import com.oneandone.sushi.fs.World;
import com.oneandone.sushi.fs.zip.ZipFilesystem;
import com.oneandone.sushi.fs.zip.ZipNode;
import com.oneandone.sushi.io.Buffer;
import com.oneandone.sushi.io.OS;
import com.oneandone.sushi.util.Program;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>File, directory, symlink or something not yet created. Replacement for java.world.File.</p>
 */
public class FileNode extends Node {
    private final FileRoot root;

    /** never null and always absolute. */
    private final File file;

    public FileNode(FileRoot root, File file) {
        if (!file.isAbsolute()) {
            throw new IllegalArgumentException(file.toString());
        }
        if (file.getPath().endsWith(File.separator) && file.getParent() != null) {
            throw new IllegalArgumentException("should not happen because java.world.File normalizes paths: " + file.getPath());
        }
        this.root = root;
        this.file = file;
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
        return file.toURI();
    }

    /** Avoid calling this method, should be used to interact with 'legacy' code only */
    public File getFile() {
        return file;
    }

    @Override
    public String getPath() {
        return file.getPath().substring(getRoot().getId().length());
    }

    public String getAbsolute() {
        return file.getAbsolutePath();
    }
    
    //--

    @Override
    public boolean exists() {
        return file.exists() || isNoneExistingBrokenLink(file);
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    public boolean canWrite() {
        return file.canWrite();
    }

    public boolean canRead() {
        return file.canRead();
    }

    public ZipNode openZip() throws IOException {
        return ((ZipFilesystem) getWorld().getFilesystem("zip")).node(this, "");
    }

    @Override
    public long length() throws LengthException {
        if (!file.isFile()) {
            throw new LengthException(this, new IOException("file expected"));
        }
        return file.length();
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        long result;

        result = file.lastModified();
        if (result == 0 && !exists()) {
            throw new GetLastModifiedException(this, new ExistsException(this, null));
        }
        return result;
    }

    @Override
    public void setLastModified(long time) throws SetLastModifiedException {
        if (!file.setLastModified(time)) {
            throw new SetLastModifiedException(this);
        }
    }

    //-- locating


    /** @return null when called for a file; non-null otherwise */
    @Override
    public List<FileNode> list() throws ListException {
        File[] children;
        List<FileNode> result;

        children = file.listFiles();
        if (children == null) {
            if (!file.canRead()) {
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
        result = new ArrayList<FileNode>(children.length);
        for (File child : children) {
            result.add(new FileNode(root, child));
        }
        return result;
    }

    //-- read and writeBytes

    @Override
    public FileInputStream createInputStream() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public FileOutputStream createOutputStream(boolean append) throws IOException {
        return new FileOutputStream(file, append);
    }

    //-- create

    /** calls createNewFile */
    @Override
    public FileNode mkfile() throws MkfileException {
    	try {
			if (!file.createNewFile()) {
			    throw new MkfileException(this);
			}
		} catch (IOException e) {
			throw new MkfileException(this, e);
		}
        return this;
    }

    @Override
    public FileNode mkdir() throws MkdirException {
        if (!file.mkdir()) {
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
            new Program(parent, "ln", "-s", target, getName()).execNoOutput();
        } catch (IOException e) {
            throw new LinkException(this, e);
        }
    }

    @Override
    public String readLink() throws ReadLinkException {
    	try {
		    return getWorld().getTemp().exec("readlink", file.getAbsolutePath()).trim();
		} catch (IOException e) {
			throw new ReadLinkException(this, e);
		}
    }

    @Override
    public boolean isLink() throws ExistsException {
    	try {
    		return isLink(file);
    	} catch (IOException e) {
    		throw new ExistsException(this, e);
    	}
    }

    private static boolean isLink(File file) throws IOException {
        String name;
        File parent;
        File canonical;

        name = file.getName();
        parent = file.getAbsoluteFile().getParentFile();
        if (parent == null) {
            // file is the root directory
            return false;
        }
        canonical = new File(parent.getCanonicalPath(), name);
        return !canonical.getAbsolutePath().equals(canonical.getCanonicalPath()) ||  isBrokenLink(file);
    }

    private static boolean isBrokenLink(File link) {
    	return link.exists() ? false : isNoneExistingBrokenLink(link);
    }

    private static boolean isNoneExistingBrokenLink(File link) {
        FilenameFilter filter;
        final String expected;
        File parent;

        parent = link.getParentFile();
        if (!parent.isDirectory()) {
            return false;
        }
    	expected = link.getName();
    	filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.equals(expected);
			}
    	};
    	return parent.listFiles(filter).length == 1;
    }

    //-- move

    /** @return dest */
    @Override
    public FileNode move(Node destNode) throws MoveException {
    	FileNode dest;
        Program p;
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
            p = new Program(dest.getParent(), "cmd", "/C", "move");
        } else {
            p = new Program(dest.getParent(), "mv");
        }
        p.add(getAbsolute(), dest.getName());
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
        rename(file, target.file);
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

    /**
     * Deletes a file or directory. Directories are deleted recursively. Handles Links.
     *
     * @throws IOException if a file cannot be deleted
     */
    @Override
    public FileNode delete() throws DeleteException {
        try {
            delete(getWorld(), file);
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    protected static void delete(World world, File file) throws IOException {
        File[] files;

        if (isLink(file)) {
            deleteLink(world, file);
            return;
        }
        files = file.listFiles();
        if (files != null) {
            for (File child : files) {
                delete(world, child);
            }
        } else {
            // not a directory
        }
        if (!file.delete()) {
            throw new FileNotFoundException("cannot delete file " + file);
        }
    }

    private static void deleteLink(World io, File link) throws IOException {
        File target; // where the link point to
        File dir;
        File renamed;
        boolean wasDeleted;

        if (!link.exists()) {
        	// broken link
        	new Program(io.getTemp(), "rm", link.getAbsolutePath()).execNoOutput();
        } else {
        	target = link.getCanonicalFile();
        	dir = target.getAbsoluteFile().getParentFile();
        	renamed = File.createTempFile("link", ".tmp", dir);
        	delete(io, renamed);
        	try {
        		rename(target, renamed);
        	} catch (IOException e) {
        		throw new IOException("Cannot delete link " + link + ": rename target " + target + " -> " + renamed
                    + " failed: " + e.getMessage());
        	}
        	wasDeleted = link.delete();
        	try {
        		rename(renamed, target);
        	} catch (IOException e) {
        		throw new IOException("Couldn't return target " + renamed + " to its original name " + target
                                  + ":\n THE RESOURCE'S NAME ON DISK HAS BEEN CHANGED BY THIS ERROR!\n" + e);
        	}
        	if (!wasDeleted) {
        		throw new IOException("Couldn't delete link: " + link + " (was it a real file? is this not a UNIX system?)");
        	}
        }
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
        return new Program(this, args).exec();
    }

    public void execNoOutput(String ... args) throws IOException {
        new Program(this, args).execNoOutput();
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
        new Program(getParent(), cmd, n, getAbsolute()).execNoOutput();
    }

    private int stat(String[] args, int radix) throws IOException {
        Program stat;

        stat = new Program(getParent(), "stat");
        stat.add(args);
        stat.add(getAbsolute());
        return Integer.parseInt(stat.exec().trim(), radix);
    }

    //--

    public FileNode createTempFile() throws IOException {
        return OnShutdown.get().createFile(this);
    }

    public FileNode createTempDirectory() throws IOException {
        return OnShutdown.get().createDirectory(this);
    }
}

