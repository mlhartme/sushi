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

import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.fs.multi.Invoker;
import net.sf.beezle.sushi.fs.multi.Target;
import net.sf.beezle.sushi.io.OS;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public abstract class NodeTest<T extends Node> extends NodeReadOnlyTest<T> {
    @Test
    public void work() throws IOException {
        List<?> children;

        assertTrue(work.exists());
        assertFalse(work.isFile());
        assertTrue(work.isDirectory());
        children = work.list();
        assertNotNull(children);
        assertEquals(0, children.size());
    }

    //-- create via world.node etc

    @Test
    public void createRoot() throws Exception {
        Node root;

        root = work.getRootNode();
        assertEquals(root, work.getWorld().node(root.getURI()));
    }

    @Test
    public void createAbsolute() throws Exception {
        Node node;

        assertEquals(work, WORLD.node(work.getURI()));
        node = work.join("foo/bar");
        assertEquals(node, WORLD.node(node.getURI()));
    }

    //--

    @Test
    public void root() {
        assertEquals(work.join("a").getRoot(), work.join("a").getRoot());
        assertEquals(work.join("a").getRoot(), work.join("ab").getRoot());
    }

    @Test
    public void rootNode() {
        Node root;

        root = work.getRootNode();
        try {
            assertTrue(root.isDirectory());
        } catch (ExistsException e) {
            // root node is not accessible (e.g. Webdav)
            // -> continue
        }
        assertEquals("", root.getName());
        assertEquals(".", root.getRelative(root));
        if (work.equals(root)) {
            assertEquals(".", work.getRelative(root));
        } else {
            assertEquals(work.getPath(), work.getRelative(root));
        }
    }

    //--

    @Test
    public void listAndBase() throws Exception {
        List<? extends Node> lst;

        work.join("foo").mkdir();
        lst = work.list();
        assertEquals(1, lst.size());
        assertEquals("foo", lst.get(0).getName());
    }

    @Test
    public void listFile() throws IOException {
        Node file;

        file = work.join("foo").writeBytes();
        assertTrue(file.isFile());
        assertNull(file.list());
    }

    @Test
    public void listNonExisting() throws IOException {
        Node nosuchfile;

        nosuchfile = work.join("nosuchfile");
        assertFalse(nosuchfile.exists());
        try {
            assertNull(nosuchfile.list());
            fail();
        } catch (ListException e) {
            assertTrue(e.getCause().getClass().getName(), e.getCause() instanceof FileNotFoundException);
        }
    }

    //--

    @Test
    public void join() throws Exception {
        assertEquals(work, work.join(""));
        assertEquals(work, work.join("a").getParent());
        assertEquals(work, work.join("x/y").getParent().getParent());
    }

    @Test
    public void joinDot() throws Exception {
        assertEquals(work, work.join("."));
        assertEquals(work.join("a"), work.join("a/."));
        assertEquals(work.join("x/y"), work.join("x/./y/."));
    }

    @Test
    public void joinDoubleDot() throws Exception {
        assertEquals(work, work.join("foo/.."));
        assertEquals(work.join("xyz"), work.join("a/./../xyz"));
        assertEquals(work.join("a/b"), work.join("x/y/../../a/b"));
    }


    // more create tests: see special char tests below

    @Test
    public void joinWithSlash() {
        try {
            work.join(Filesystem.SEPARATOR, "a");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    @Test
    public void parent() {
        assertEquals(work, work.join("a").getParent());
        assertEquals("a", work.join("a/b").getParent().getName());
    }

    @Test
    public void anchestor() {
        Node file;

        file = work.join("foo/bar");
        assertFalse(file.hasDifferentAnchestor(file));
        assertTrue(file.hasAnchestor(file));
        assertTrue(file.hasAnchestor(file.getParent()));
        assertTrue(file.hasAnchestor(work));
    }

    @Test
    public void relative() {
        Node parent;
        Node file;

        parent = work.join("foo");
        file = parent.join("bar");
        assertEquals(".", file.getRelative(file));
        assertEquals("bar", file.getRelative(parent));
        assertEquals("foo/bar", file.getRelative(work));
        assertEquals("../foo/bar", file.getRelative(work.join("baz")));
        assertEquals("../bar", file.getRelative(work.join("foo/baz")));
    }

    @Test
    public void nameAndPath() throws IOException {
        Node node;

        node = work.join("foo");
        assertFalse(node.exists());
        assertTrue(node.getPath().endsWith("foo"));
        assertEquals("foo", node.getName());
        node = work.join("a/b");
        assertFalse(node.exists());
        assertTrue(node.getPath().endsWith("a/b"));
        assertEquals("b", node.getName());
        node = work.join("x/y/z");
        assertFalse(node.exists());
        assertTrue(node.getPath().endsWith("x/y/z"));
        assertEquals("z", node.getName());
    }

    @Test
    public void hidden() throws IOException {
        List<? extends Node> files;

        work.join(".dotfile").writeString("foo");
        files = work.list();
        assertEquals(1, files.size());
        assertEquals(".dotfile", files.get(0).getName());
    }

    //-- status methods: exists, isFile, isDirectory

    @Test
    public void statusFile() throws IOException {
        Node file;

        file = work.join("foo");
        assertFalse(file.exists());
        assertFalse(file.isFile());
        assertFalse(file.isDirectory());
        file.writeBytes();
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());
        file.delete();
        assertFalse(file.exists());
        assertFalse(file.isFile());
        assertFalse(file.isDirectory());
    }

    @Test
    public void statusDirectory() throws IOException {
        Node dir;

        dir = work.join("foo");
        assertFalse(dir.exists());
        assertFalse(dir.isFile());
        assertFalse(dir.isDirectory());
        dir.mkdir();
        assertTrue(dir.exists());
        assertFalse(dir.isFile());
        assertTrue(dir.isDirectory());
        dir.delete();
        assertFalse(dir.exists());
        assertFalse(dir.isFile());
        assertFalse(dir.isDirectory());
    }

    //--

    //

    @Test
    public void modifiedFile() throws Exception {
        Node file;
        long modified;

        file = work.join("file");
        assertFalse(file.exists());
        try {
            file.getLastModified();
            fail();
        } catch (GetLastModifiedException e) {
            // ok
        }
        file.writeBytes();
        modified = file.getLastModified();
        sameTime(modified, System.currentTimeMillis());
        file.readString();
        assertEquals(modified, file.getLastModified());
        file.writeString("");
        assertTrue(file.getLastModified() >= modified);
        modified = System.currentTimeMillis() - 1000 * 60 * 5;
        try {
            file.setLastModified(modified);
        } catch (SetLastModifiedException e) {
            // setLastModified is not supported - ignore
            return;
        }
        sameTime(modified, file.getLastModified());
    }

    @Test
    public void modifiedDirectory() throws Exception {
        Node dir;
        long modified;

        dir = work.join("dir");
        dir.mkdir();
        sameTime(dir.getLastModified(), System.currentTimeMillis());
        modified = System.currentTimeMillis() - 1000 * 60 * 5;
        try {
            dir.setLastModified(modified);
        } catch (SetLastModifiedException e) {
            // setLastModified is not supported - ignore
            return;
        }
        sameTime(modified, dir.getLastModified());
    }

    private static void sameTime(long left, long right) {
        if (Math.abs(left - right) > 2000) {
            fail("expected: " + time(left) + ", got " + time(right));
        }
    }
    private static String time(long time) {
        return time + " (" + new Date(time) + ")";
    }

    //-- read/write

    @Test
    public void readNonexisting() throws IOException {
        Node file;

        file = work.join("doesnotexist");
        assertFalse(file.exists());
        try {
            file.createInputStream();
            fail();
        } catch (FileNotFoundException e) {
            // ok
        }
    }

    @Test
    public void readDirectory() throws IOException {
        Node dir;

        dir = work.join("dir");
        dir.mkdir();
        assertTrue(dir.isDirectory());
        try {
            dir.createInputStream();
            fail();
        } catch (FileNotFoundException e) {
            // ok
        }
    }

    public void lengthFile() throws IOException {
        Node file;

        file = work.join("file");
        file.writeBytes();
        assertEquals(0, file.length());
        file.writeBytes((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, file.length());
    }

    @Test(expected=LengthException.class)
    public void lengthDirectory() throws IOException {
        Node dir;

        dir = work.join("dir");
        dir.mkdir();
        dir.length();
    }
    @Test(expected=LengthException.class)
    public void lengthNotFound() throws IOException {
        Node dir;

        dir = work.join("dir");
        dir.length();
    }

    @Test
    public void readEmpty() throws IOException {
        Node file;
        byte[] data;
        byte[] data1 = {};

        file = work.join("foo").writeBytes(data1);
        data = file.readBytes();
        assertEquals(0L, file.length());
        assertNotNull(data);
        assertEquals(0, data.length);
    }

    @Test
    public void readNormal() throws IOException {
        Node file;

        file = work.join("foo").writeString("some data");
        assertEquals("some data", file.readString());
        assertEquals(9L, file.length());
        // read again
        assertEquals("some data", file.readString());
    }

    @Test
    public void readerWriter() throws IOException {
        Node file;
        NodeWriter writer;
        NodeReader reader;

        file = work.join("foo");
        writer = file.createWriter();
        assertSame(file, writer.getNode());
        writer.write("hi");
        writer.close();

        reader = file.createReader();
        assertSame(file, reader.getNode());
        assertEquals('h', reader.read());
        assertEquals('i', reader.read());
        assertEquals(-1, reader.read());
        reader.close();
    }

    @Test
    public void specialNames() throws IOException {
    	if (OS.CURRENT == OS.WINDOWS) {
    		return; // TODO: many failures ...
    	}
        for (char c = 32; c < 127; c++) {
            try {
                if (c >= '0' && c <='9') {
                    // skip
                } else if (c >= 'a' && c <='z') {
                    // skip
                } else if (c >= 'A' && c <='Z') {
                    // skip
                } else if (c == '/') {
                    // skip
                } else if (c == '*') { // TODO: still fail in jsch 0.1.44-1, rm /tmp/sushisshtests to fix
                    // skip
                } else if (c == '?') { // TODO: still fail in jsch 0.1.44-1, rm /tmp/sushisshtests to fix
                    // skip
                } else if (c == '\\') {
                    // skip
                } else {
                    checkFilename("before" + c + "after" + c);
                    checkDirectory("before" + c + "after" + c);
                }
            } catch (Throwable e) {
                throw new IOException("specialName failed: " + c + "=" + ((int) c) + ": " + e.getMessage(), e);
            }
        }
    }

    private void checkFilename(String name) throws Exception {
        final String content = "abc";
        Node file;
        Node alias;

        file = work.join(name);
        file.writeString(content);
        assertEquals(content, file.readString());
        assertEquals(Collections.singletonList(file), file.getParent().list());
        alias = WORLD.node(file.getURI());
        assertEquals(file, alias);
        assertEquals(content, alias.readString());
        file.delete();
    }

    private void checkDirectory(String name) throws IOException {
        Node file;

        file = work.join(name);
        file.mkdir();
        assertTrue(file.isDirectory());
        assertEquals(Collections.singletonList(file), file.getParent().list());
        file.delete();
    }

    private static final String ALL_CHARS;

    static {
        StringBuilder builder;

        builder = new StringBuilder(Character.MIN_HIGH_SURROGATE);
        for (int i = 0; i < Character.MIN_HIGH_SURROGATE; i++) {
            builder.append((char) i);
        }
        ALL_CHARS = builder.toString();
    }

    @Test
    public void readerEncoding() throws IOException {
        Node file;
        Reader src;
        int c;
        StringBuilder str;
        int max;
        int left;
        int right;

        file = work.join("foo");
        file.writeBytes(ALL_CHARS.getBytes(file.getWorld().getSettings().encoding));
        src = file.createReader();
        str = new StringBuilder();
        while (true) {
            c = src.read();
            if (c == -1) {
                break;
            }
            str.append((char) c);
        }
        max = Math.max(ALL_CHARS.length(), str.length());
        for (int i = 0; i < max; i++) {
            left = i < ALL_CHARS.length() ? ALL_CHARS.charAt(i) : -1;
            right = i < str.length() ? str.charAt(i) : - 1;
            assertEquals("idx=" + i, left, right);
        }
    }

    @Test
    public void writerEncoding() throws IOException {
        Node file;
        Writer dest;

        file = work.join("foo");
        dest = file.createWriter();
        dest.write(ALL_CHARS);
        dest.close();
        assertTrue(Arrays.equals(ALL_CHARS.getBytes(file.getWorld().getSettings().encoding), file.readBytes()));
    }

    @Test
    public void readWriteString() throws IOException {
        Node file;

        file = work.join("foo");
        file.writeString("");
        assertTrue(file.exists());
        assertEquals("", file.readString());
        file.writeString("more");
        assertEquals("more", file.readString());
    }


    @Test
    public void append() throws IOException {
        Node file;

        file = work.join("foo");
        file.appendBytes((byte) 97, (byte) 98);
        file.appendLines("", "xyz");
        file.appendString("1");
        file.appendChars('A', 'B');
        assertEquals("ab" + OS.CURRENT.lineSeparator + "xyz" +
        		OS.CURRENT.lineSeparator + "1AB", file.readString());
    }

    @Test
    public void readWriteObject() throws IOException {
        final String obj = "hello";
        Node file;

        file = work.join("foo");
        file.writeObject(obj);
        assertTrue(file.exists());
        assertFalse(obj.equals(file.readString()));
        assertEquals(obj, file.readObject());
    }

    @Test
    public void readWriteLines() throws IOException {
        final String[] data = { "", " ", "a", "\t a\r", "hello, world" };
        Node file;

        file = work.join("foo");
        file.writeLines(data);
        assertEquals(Arrays.asList(data), file.readLines());
    }

    @Test
    public void readWriteXml() throws IOException, SAXException {
        Document doc;
        Node file;

        doc = WORLD.getXml().getBuilder().literal("<a><b/></a>");
        file = work.join("foo");
        file.writeXml(doc);
        assertEquals(WORLD.getSettings().join("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<a>", "<b/>", "</a>", ""), file.readString());
        doc = file.readXml();
        assertEquals("a", doc.getDocumentElement().getLocalName());
    }

    @Test
    public void writeToNonexistingDirectory() {
        try {
            work.join("nosuchdir/file").writeString("");
            fail();
        } catch (IOException e) {
            // ok
        }
    }

    @Test
    public void writeOverExistingFile() throws IOException {
        Node file;

        file = work.join("existing");
        file.writeString("foo");
        file.createOutputStream().close();
        assertEquals("", file.readString());
    }

    @Test
    public void inputAlreadyClosed() throws IOException {
        Node file;
        InputStream in;

        file = work.join("file");
        file.writeString("foo");
        in = file.createInputStream();
        assertEquals('f', in.read());
        in.close();
        try {
            in.read();
            fail();
        } catch (IllegalStateException e) {
            // ok
        } catch (IOException e) {
            // ok
        }
    }

    @Test
    public void inputClosedTwice() throws IOException {
        Node file;
        InputStream in;

        file = work.join("file");
        file.writeString("foo");
        in = file.createInputStream();
        assertEquals('f', in.read());
        in.close();
        in.close();
    }

    @Test
    public void outputAlreadyClosed() throws IOException {
        OutputStream out;

        out = work.join("file").createOutputStream();
        out.write(1);
        out.close();
        try {
            out.write(1);
            fail();
        } catch (IllegalStateException e) {
            // ok
        } catch (IOException e) {
            // ok
        }
    }

    @Test
    public void outputClosedTwice() throws IOException {
        OutputStream out;

        out = work.join("file").createOutputStream();
        out.write(1);
        out.close();
        out.close();
    }

    @Test
    public void multipleInputOutputs() throws Exception {
        Node node1;
        Node node2;
        OutputStream out1;
        OutputStream out2;
        InputStream in1;
        InputStream in2;

        node1 = work.join("1");
        node2 = work.join("2");
        out1 = node1.createOutputStream();
        out1.write('a');
        out2 = node2.createOutputStream();
        out2.write('1');
        out2.write('2');
        out1.write('b');
        out1.close();
        out2.close();

        in2 = node2.createInputStream();
        assertEquals('1', in2.read());
        in1 = node1.createInputStream();
        assertEquals('a', in1.read());
        assertEquals('b', in1.read());
        assertEquals('2', in2.read());
        in1.close();
        in2.close();
    }

    //-- mkfile

    @Test
    public void mkfileNormal() throws IOException {
        Node file;

        file = work.join("file");
        assertSame(file, file.mkfile());
        assertTrue(file.exists());
        assertTrue(file.isFile());
        assertFalse(file.isDirectory());
    }

    @Test(expected=MkfileException.class)
    public void mkfileToNonexistingDirectory() throws IOException {
        work.join("nosuchdir/file").mkfile();
    }

    @Test(expected=MkfileException.class)
    public void mkfileOnFile() throws IOException {
        Node file;

        file = work.join("file");
        file.mkfile();
        file.mkfile();
    }

    @Test(expected=MkfileException.class)
    public void mkfileOnDir() throws IOException {
        work.mkfile();
    }

    //-- mkdir

    @Test
    public void mkdir() throws IOException {
        Node dir;

        dir = work.join("dir");
        assertSame(dir, dir.mkdir());
        assertTrue(dir.exists());
        assertFalse(dir.isFile());
        assertTrue(dir.isDirectory());
    }

    @Test(expected=MkdirException.class)
    public void mkdirToNonexistingDirectory() throws IOException {
        work.join("nosuchdir/file").mkdir();
    }

    @Test(expected=MkdirException.class)
    public void mkdirOnExisting() throws IOException {
        work.mkdir();
    }

    @Test(expected=MkdirException.class)
    public void mkdirsOnExisting() throws IOException {
        work.mkdirs();
    }

    @Test
    public void mkdirs() throws IOException {
        Node dir;

        dir = work.join("dir/sub");
        dir.mkdirs();
        assertTrue(dir.isDirectory());
        try {
            dir.mkdirs();
            fail();
        } catch (IOException e) {
            // ok
        }
        assertTrue(dir.isDirectory());
    }

    @Test
    public void mkdirsOpt() throws IOException {
        Node dir;

        dir = work.join("dir/sub");
        dir.mkdirsOpt();
        assertTrue(dir.isDirectory());
        dir.mkdirsOpt();
        assertTrue(dir.isDirectory());
    }

    @Test(expected=MkdirException.class)
    public void mkdirOptOverFile() throws IOException {
        Node file;

        file = work.join("file").writeBytes();
        file.mkdirOpt();
    }

    @Test(expected=MkdirException.class)
    public void mkdirsOptOverFile() throws IOException {
        Node file;

        file = work.join("file").writeBytes();
        file.mkdirsOpt();
    }

    //-- delete method

    @Test
    public void deleteFile() throws IOException {
        Node node;
        byte[] data = {};

        node = work.join("myfile");
        try {
            node.delete();
            fail();
        } catch (DeleteException e) {
            // ok
        }
        node.writeBytes(data);
        node.delete();
        assertFalse(node.exists());
        try {
            node.delete();
            fail();
        } catch (DeleteException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }
    }

    @Test
    public void deleteDirectory() throws IOException {
        Node dir;
        Node child;

        dir = work.join("mydir");
        try {
            dir.delete();
            fail();
        } catch (DeleteException e) {
            // ok
        }
        dir.mkdir();
        child = dir.join("file").writeBytes();

        dir.delete();
        assertFalse(dir.exists());
        assertFalse(child.exists());
        try {
            dir.delete();
            fail();
        } catch (DeleteException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }
    }

    //-- move

    @Test
    public void moveDirectory() throws IOException {
        doMove(work.join("old").mkdir(), work.join("moved"));
    }

    @Test
    public void moveFile() throws IOException {
        doMove((work.join("old")).mkfile(), work.join("moved"));
    }

    @Test
    public void moveToExistingDir() throws IOException {
        Node destdir;

        destdir = work.join("subdir").mkdir();
        doMove((work.join("old")).mkfile(), destdir.join("moved"));
    }

    @Test(expected=IOException.class)
    public void moveToNonexistingDir() throws IOException {
        doMove((work.join("old")).mkfile(), work.join("nosuchdir/moved"));
    }

    @Test(expected=IOException.class)
    public void moveOverExisting() throws IOException {
        Node dest;

        dest = work.join("moved").mkfile();
        doMove(work.join("old").mkfile(), dest);
    }

    @Test(expected=IOException.class)
    public void moveToSame() throws IOException {
        Node node;

        node = work.join("old").mkdir();
        doMove(node, node);
    }

    private void doMove(Node src, Node dest) throws IOException {
        assertSame(dest, src.move(dest));
        src.checkNotExists();
        dest.checkExists();
    }


    //-- other ops

    @Test
    public void gzip() throws IOException {
        final String str = "1234567890abc";
        Node normal;
        Node gzip;
        Node gunzip;

        normal = work.join("foo");
        gzip = work.join("foo.gz");
        gunzip = work.join("foo.gunzip");

        normal.writeString(str);
        normal.gzip(gzip);
        assertTrue(normal.diff(gzip));
        gzip.gunzip(gunzip);
        assertFalse(normal.diff(gunzip));
        assertEquals(str, gunzip.readString());
    }

    @Test
    public void md5() throws IOException {
        Node a;
        String digest;

        a = work.join("a");
        a.writeBytes();
        digest = a.md5();
        // string was computed my md5sum:
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", digest);
        a.writeBytes((byte) 0);
        assertFalse(digest.equals(a.md5()));
    }

    @Test
    public void diff() throws IOException {
        Node left = work.join("left");
        Node right = work.join("right");
        left.writeString("a");
        right.writeString("a");
        assertFalse(left.diff(right));
        right.writeString("b");
        assertTrue(left.diff(right));
    }

    //-- copy

    @Test
    public void copyFile() throws IOException {
        Node a;
        Node b;

        a = work.join("a");
        b = work.join("b");
        a.writeString("xy");
        assertFalse(b.exists());
        a.copyFile(b);
        assertTrue(b.exists());
        assertEquals("xy", b.readString());
        a.writeString("123");
        a.copyFile(b);
        assertEquals("123", b.readString());
    }

    @Test
    public void copyDirectory() throws IOException {
        Node src;
        Node dest;

        src = work.join("src").mkdir();
        src.join("a").writeString("A");
        src.join("b").writeString("B");
        src.join("dir").mkdir();
        dest = work.join("dest");
        dest.mkdir();
        assertEquals(3, src.copyDirectory(dest).size());
        assertEquals("A", dest.join("a").readString());
        assertEquals("B", dest.join("b").readString());
        dest.join("dir").checkDirectory();
    }

    @Test
    public void mode() throws Exception {
        Node file;

        if (!work.getRoot().getFilesystem().getFeatures().modes) {
            return;
        }
        file = work.join("file");
        file.writeBytes();
        file.setMode(0644);
        assertEquals(0644, file.getMode());
        file.setMode(0755);
        assertEquals(0755, file.getMode());
    }

    @Test
    public void uidDir() throws Exception {
        doUid(work.join("dir").mkdir());
    }
    @Test
    public void uidFile() throws Exception {
        doUid(work.join("file").writeBytes());
    }

    private void doUid(Node node) throws IOException {
        int id;

        if (!work.getRoot().getFilesystem().getFeatures().modes) {
            return;
        }
        id = node.getUid();
        node.setUid(id);
        assertEquals(id, node.getUid());
        try {
            node.setUid(0);
            fail();
        } catch (IOException e) {
            // ok
        }
        assertEquals(id, node.getUid());
    }

    @Test
    public void gidDir() throws Exception {
        doGid(work.join("dir").mkdir());
    }
    @Test
    public void gidFile() throws Exception {
        doGid(work.join("file").writeBytes());
    }

    private void doGid(Node node) throws IOException {
        int id;

        if (!work.getRoot().getFilesystem().getFeatures().modes) {
            return;
        }
        id = node.getGid();
        node.setGid(id);
        assertEquals(id, node.getGid());
        if (id == 0) {
        	return;
        }
        assertEquals(id, node.getGid());
    }

    //-- Object methods

    @Test
    public void equal() {
        assertEquals(work, work.join());
        assertEquals(work.join("a"), work.join("a"));
        assertEquals(work.join("no/such/file"), work.join("no/such/file"));
        assertFalse(work.equals(work.join("a")));
        assertFalse(work.join("a").equals(work.join("b")));
    }

    @Test
    public void toStr() {
        Node orig;
        Node node;

        orig = work.getWorld().getWorking();
        try {
            work.getWorld().setWorking(work);
            node = work.join("foo");
            assertEquals(".", work.toString());
            assertEquals("foo", node.toString());
            work.getWorld().setWorking(null);
            if (node instanceof FileNode) {
                assertEquals(((FileNode) node).getFile().toString(), node.toString());
            } else {
                assertEquals(node.getURI().toString(), node.toString());
            }
        } finally {
            work.getWorld().setWorking(orig);
        }
    }

    //-- links
    //--

    private boolean canLink() {
        return work.getRoot().getFilesystem().getFeatures().links;
    }

    @Test
    public void linkAbsolute() throws IOException {
        Node orig;
        Node link;

        if (!canLink()) {
            return;
        }

        orig = work.join("orig").writeString("first");
        link = work.join("link");

        assertTrue(orig.isFile());
        // TODO http://bugs.sun.com/view_bug.do?bug_id=5085227
        //assertFalse(orig.isLink());
        assertFalse(link.exists());
        //assertFalse(link.isLink());

        orig.link(link);
        assertTrue(link.exists());
        assertTrue(link.isLink());
        assertEquals(orig, link.resolveLink());
        assertEquals("/" + work.getPath() + "/orig", link.readLink());

        assertEquals("first", link.readString());
        orig.writeString("second");
        assertEquals("second", link.readString());
        link.writeString("third");
        assertEquals("third", orig.readString());

        link.delete();
        assertTrue(orig.exists());
        assertFalse(link.exists());
        assertFalse(link.isLink());
    }

    @Test
    public void linkRelative() throws IOException {
        Node orig;
        Node link;

        if (!canLink()) {
            return;
        }

        orig = work.join("orig").writeString("first");
        link = work.join("link");

        assertTrue(orig.isFile());
        // TODO http://bugs.sun.com/view_bug.do?bug_id=5085227
        //assertFalse(orig.isLink());
        assertFalse(link.exists());
        //assertFalse(link.isLink());

        link.mklink(orig.getName());
        assertEquals(orig.getName(), link.readLink());
        assertTrue(link.exists());
        assertTrue(link.isLink());
        assertEquals(orig, link.resolveLink());

        link.delete();
        assertTrue(orig.exists());
        assertFalse(link.exists());
        assertFalse(link.isLink());
    }

    @Test
    public void mklinkBroken() throws IOException {
        Node link;

        if (!canLink()) {
            return;
        }

        link = work.join("link");
        link.mklink("nosuchfile");

        assertTrue(link.exists());
        assertTrue(link.isLink());
        assertFalse(link.isFile());
        assertFalse(link.isDirectory());

        link.delete();
        assertFalse(link.exists());
        assertFalse(link.isLink());
    }

    @Test(expected=LinkException.class)
    public void mklinkDoNotOverwrite() throws IOException {
        Node link;

        if (!canLink()) {
            throw new LinkException(work, null);
        }

        link = work.join("link");
        link.mkfile();
        link.mklink("..");
    }

    @Test
    public void linkDelete() throws IOException {
        Node file;
        Node link;

        if (!canLink()) {
            return;
        }

        file = work.join("file");
        file.writeString("foo");
        link = work.join("link");
        file.link(link);
        assertEquals("foo", link.readString());
        link.delete();
        assertFalse(link.isFile());
        assertTrue(file.isFile());
    }


    //-- multi-threading tests

    @Test
    public void multiThreading() throws Exception {
        Invoker.runAll(5, 100, new Target(work));
    }
}
