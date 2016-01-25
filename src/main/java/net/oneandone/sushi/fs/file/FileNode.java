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
package net.oneandone.sushi.fs.file;

import net.oneandone.sushi.archive.Archive;
import net.oneandone.sushi.fs.CopyFileFromException;
import net.oneandone.sushi.fs.CopyFileToException;
import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.Filesystem;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.LinkException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.MkfileException;
import net.oneandone.sushi.fs.ModeException;
import net.oneandone.sushi.fs.MoveException;
import net.oneandone.sushi.fs.NewDirectoryOutputStreamException;
import net.oneandone.sushi.fs.NewInputStreamException;
import net.oneandone.sushi.fs.NewOutputStreamException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.ReadLinkException;
import net.oneandone.sushi.fs.SetLastModifiedException;
import net.oneandone.sushi.fs.SizeException;
import net.oneandone.sushi.fs.zip.ZipFilesystem;
import net.oneandone.sushi.fs.zip.ZipNode;
import net.oneandone.sushi.io.Buffer;
import net.oneandone.sushi.launcher.Launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a file, directory, link or something not yet created. Replacement for java.io.File or java.nio.Path. </p>
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
     * Avoid calling this method in your code. Should only be used to interact with code that does not know Sushi (File) Nodes.
     */
    public Path toPath() {
        return path;
    }

    /** does not include the drive on windows */
    @Override
    public String getPath() {
    	String result;

    	result = path.toString().substring(getRoot().getAbsolute().length());
    	return result.replace(File.separatorChar, Filesystem.SEPARATOR_CHAR);
    }

    public String getAbsolute() {
        return path.toString();
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

    /**
     * @param dest resulting zip file
     * @return dest
     */
    public Node zip(Node dest) throws IOException {
        Archive archive;

        archive = new Archive(this, null, getPath() + "/");
        archive.save(dest);
        return dest;
    }

    /**
     * @param dest resulting directory
     * @return dest
     */
    public Node unzip(Node dest) throws IOException {
        openZip().copyDirectory(dest);
        return this;
    }

    public Node unjar(Node dest) throws IOException {
        openJar().copyDirectory(dest);
        return this;
    }


    @Override
    public long size() throws SizeException {
        try {
            checkFile();
            return Files.size(path);
        } catch (IOException e) {
            throw new SizeException(this, e);
        }
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new GetLastModifiedException(this, e);
        }
    }

    @Override
    public void setLastModified(long time) throws SetLastModifiedException {
        try {
            Files.setLastModifiedTime(path, FileTime.fromMillis(time));
        } catch (IOException e) {
            throw new SetLastModifiedException(this, e);
        }
    }

    //-- locating


    /** @return null when called for a file; non-null otherwise */
    @Override
    public List<FileNode> list() throws ListException, DirectoryNotFoundException {
        List<FileNode> result;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
            result = new ArrayList<>();
            for (Path child : ds) {
                result.add(new FileNode(root, child));
            }
            return result;
        } catch (IOException e) {
            if (isFile()) {
                return null;
            }
            if (!exists()) {
                throw new DirectoryNotFoundException(this, e);
            }
            throw new ListException(this, e);
        }
    }

    //-- read and writeBytes

    @Override
    public InputStream newInputStream() throws FileNotFoundException, NewInputStreamException {
        if (isDirectory()) {
            throw new FileNotFoundException(this, "file not found - this is a directory");
        }
        try {
            return Files.newInputStream(path);
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException(this, e);
        } catch (IOException e) {
            throw new NewInputStreamException(this, e);
        }
    }

    public long copyFileTo(OutputStream dest, long skip) throws FileNotFoundException, CopyFileToException {
        return copyFileToImpl(dest, skip);
    }

    public void copyFileFrom(InputStream dest) throws FileNotFoundException, CopyFileFromException {
        copyFileFromImpl(dest);
    }


    @Override
    public OutputStream newOutputStream(boolean append) throws NewOutputStreamException {
        try {
            if (append) {
                return Files.newOutputStream(path, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            } else {
                return Files.newOutputStream(path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE );
            }
        } catch (IOException e) {
            if (isDirectory()) {
                throw new NewDirectoryOutputStreamException(this, e);
            }
            throw new NewOutputStreamException(this, e);
        }
    }

    /**
     * Determines the files size to allocate the resulting array in one chunk. More efficient than the
     * default implementation.
     */
    @Override
    public byte[] readBytes() throws IOException {
        return Files.readAllBytes(path);
    }

    //-- create

    /** calls createNewFile */
    @Override
    public FileNode mkfile() throws MkfileException {
    	try {
            Files.createFile(path);
		} catch (IOException e) {
			throw new MkfileException(this, e);
		}
        return this;
    }

    @Override
    public FileNode mkdir() throws MkdirException {
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            throw new MkdirException(this);
        }
        return this;
    }

    @Override
    public void mklink(String target) throws LinkException {
        try {
            checkNotExists();
            getParent().checkDirectory();
            Files.createSymbolicLink(path, path.getFileSystem().getPath(target));
        } catch (IOException e) {
            throw new LinkException(this, e);
        }
    }

    @Override
    public String readLink() throws ReadLinkException {
    	try {
            return Files.readSymbolicLink(path).toString();
        } catch (IOException e) {
			throw new ReadLinkException(this, e);
		}
    }

    @Override
    public boolean isLink() throws ExistsException {
        return Files.isSymbolicLink(path);
    }

    //-- move

    /**
     * Uses copy/remove to move between file systems.
     * @return dest
     */
    @Override
    public Node move(Node destNode, boolean overwrite) throws NodeNotFoundException, MoveException {
    	FileNode dest;

        if (!(destNode instanceof FileNode)) {
            return super.move(destNode, overwrite);
        }
        dest = (FileNode) destNode;
        if (!overwrite) {
      	    try {
          		dest.checkNotExists();
          	} catch (IOException e) {
      	    	throw new MoveException(this, dest, "dest exists", e);
      	    }
        }
        try {
            Files.move(path, dest.path, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            return super.move(destNode, overwrite);
        } catch (NoSuchFileException e) {
            throw new FileNotFoundException(this, e);
		} catch (IOException e) {
			throw new MoveException(this, dest, "os command failed", e);
		}
        return dest;
    }

    /** Returns a launcher with working directory this. */
    public Launcher launcher(String ... args) {
        return new Launcher(this, args);
    }

    //-- delete

    public FileNode deleteFile() throws DeleteException, FileNotFoundException {
        try {
            checkFile(); // expensive, but I have to avoid deleting empty directories
            Files.delete(path);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    public FileNode deleteDirectory() throws DeleteException, DirectoryNotFoundException {
        try {
            checkDirectory(); // expensive, but I have to avoid deleting files
            Files.delete(path);
        } catch (DirectoryNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    /**
     * Deletes a file or directory. Directories are deleted recursively. Handles Links.
     */
    @Override
    public FileNode deleteTree() throws DeleteException, NodeNotFoundException {
        if (!exists()) {
            throw new NodeNotFoundException(this);
        }
        try {
            doDeleteTree(path);
        } catch (IOException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    protected static void doDeleteTree(Path path) throws IOException {
        if (!Files.isSymbolicLink(path)) {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path child : stream) {
                        doDeleteTree(child);
                    }
                }
            }
        }
        Files.delete(path);
    }

    //--

    @Override
    public boolean diff(Node right, Buffer rightBuffer) throws IOException {
        if (right instanceof FileNode) {
            if (size() != right.size()) {
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
    public String getPermissions() throws ModeException {
        return PosixFilePermissions.toString(attributes().permissions());
    }

    @Override
    public void setPermissions(String permissions) throws ModeException {
        try {
            Files.getFileAttributeView(path, PosixFileAttributeView.class).setPermissions(
                    PosixFilePermissions.fromString(permissions));
        } catch (IOException e) {
            throw new ModeException(this, e);
        }
    }

    @Override
    public UserPrincipal getOwner() throws ModeException {
        return attributes().owner();
    }

    @Override
    public void setOwner(UserPrincipal owner) throws ModeException {
        FileOwnerAttributeView view = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        try {
            view.setOwner(owner);
        } catch (IOException e) {
            throw new ModeException(this, e);
        }
    }

    @Override
    public GroupPrincipal getGroup() throws ModeException {
        return attributes().group();
    }

    @Override
    public void setGroup(GroupPrincipal group) throws ModeException {
        PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        try {
            view.setGroup(group);
        } catch (IOException e) {
            throw new ModeException(this, e);
        }
    }

    private PosixFileAttributes attributes() throws ModeException {
        try {
            return Files.readAttributes(path, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new ModeException(this, e);
        }
    }

    //--

    @Override
    public String toString() {
        Node working;

        working = getWorld().getWorking();
        if (working != null && hasAnchestor(working)) {
            return getRelative(working).replace(Filesystem.SEPARATOR_CHAR, File.separatorChar);
        } else {
            return path.toString();
        }
    }

    //--

    public FileNode createTempFile() throws IOException {
        return getWorld().onShutdown().createFile(this);
    }

    public FileNode createTempDirectory() throws IOException {
        return getWorld().onShutdown().createDirectory(this);
    }
}

