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
package net.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import net.oneandone.sushi.fs.CreateInputStreamException;
import net.oneandone.sushi.fs.CreateOutputStreamException;
import net.oneandone.sushi.fs.DeleteException;
import net.oneandone.sushi.fs.DirectoryNotFoundException;
import net.oneandone.sushi.fs.ExistsException;
import net.oneandone.sushi.fs.FileNotFoundException;
import net.oneandone.sushi.fs.GetLastModifiedException;
import net.oneandone.sushi.fs.LengthException;
import net.oneandone.sushi.fs.LinkException;
import net.oneandone.sushi.fs.ListException;
import net.oneandone.sushi.fs.MkdirException;
import net.oneandone.sushi.fs.ModeException;
import net.oneandone.sushi.fs.MoveException;
import net.oneandone.sushi.fs.Node;
import net.oneandone.sushi.fs.NodeAlreadyExistsException;
import net.oneandone.sushi.fs.NodeException;
import net.oneandone.sushi.fs.NodeNotFoundException;
import net.oneandone.sushi.fs.ReadFromException;
import net.oneandone.sushi.fs.ReadLinkException;
import net.oneandone.sushi.fs.SetLastModifiedException;
import net.oneandone.sushi.fs.WriteToException;
import net.oneandone.sushi.fs.file.FileNode;
import net.oneandone.sushi.io.CheckedByteArrayOutputStream;
import net.oneandone.sushi.launcher.ExitCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.List;

public class SshNode extends Node {
    private final SshRoot root;
    private final String slashPath;

    public SshNode(SshRoot root, String path) {
        if (root == null) {
            throw new IllegalArgumentException();
        }
        if (path.startsWith("/")) {
            throw new IllegalArgumentException();
        }
        this.root = root;
        this.slashPath = "/" + path;
    }

    @Override
    public URI getURI() {
        try {
            return new URI(root.getFilesystem().getScheme(), root.getUser(), root.getHost(), -1, slashPath, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public SshRoot getRoot() {
        return root;
    }

    @Override
    public SshNode getParent() {
        return (SshNode) doGetParent();
    }

    @Override
    public SshNode join(String ... paths) {
        return (SshNode) doJoin(paths);
    }

    @Override
    public SshNode join(List<String> paths) {
        return (SshNode) doJoin(paths);
    }

    private static String escape(String str) {
        StringBuilder builder;
        int len;
        char c;

        builder = new StringBuilder();
        len = str.length();
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            switch (c) {
                case '*':
                case '?':
                case '\\':
                    builder.append('\\');
                    builder.append(c);
                    break;
                default:
                    builder.append(c);
                    // do nothing
            }
        }
        return builder.toString();
    }

    private static String unescape(String str) {
        StringBuilder builder;
        int len;
        char c;

        builder = new StringBuilder();
        len = str.length();
        for (int i = 0; i < len; i++) {
            c = str.charAt(i);
            switch (c) {
                case '\\':
                    builder.append(str.charAt(++i));
                    break;
                default:
                    builder.append(c);
                    // do nothing
            }
        }
        return builder.toString();
    }

    @Override
    public long length() throws LengthException {
        ChannelSftp sftp;
        SftpATTRS attrs;

        try {
            sftp = alloc();
            try {
                attrs = sftp.stat(escape(slashPath));
                if (attrs.isDir()) {
                    throw new LengthException(this, new IOException("file expected"));
                }
                return attrs.getSize();
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            throw new LengthException(this, e);
        }
    }

    @Override
    public String getPath() {
        return slashPath.substring(1);
    }

    //--

    @Override
    public List<SshNode> list() throws DirectoryNotFoundException, ListException {
        List<SshNode> nodes;
        ChannelSftp.LsEntry entry;
        String name;
        boolean dir;
        ChannelSftp sftp;

        try {
            nodes = new ArrayList<>();
            dir = false;
            sftp = alloc();
            try {
                for (Object obj : sftp.ls(escape(slashPath))) {
                    try {
                        entry = (ChannelSftp.LsEntry) obj;
                        name = entry.getFilename();
                        if (".".equals(name) || "..".equals(name)) {
                            dir = true;
                        } else {
                            nodes.add(join(name));
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("illegal name: " + obj, e);
                    }
                }
                if (!dir && nodes.size() == 1) {
                    return null;
                } else {
                    return nodes;
                }
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            if (e.id == 2) {
                throw new DirectoryNotFoundException(this);
            }
            throw new ListException(this, e);
        } catch (JSchException e) {
            throw new ListException(this, e);
        }
    }

    //--

    @Override
    public SshNode deleteFile() throws FileNotFoundException, DeleteException {
        ChannelSftp sftp;
        boolean directory;

        try {
            sftp = alloc();
        } catch (JSchException e) {
            throw new DeleteException(this, e);
        }
        try {
            sftp.rm(escape(slashPath));
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                throw new FileNotFoundException(this);
            }
            try {
                directory = isDirectory();
            } catch (ExistsException e1) {
                directory = false;
                // fall-through - report original exception
            }
            if (directory) {
                throw new FileNotFoundException(this, e);
            }
            throw new DeleteException(this, e);
        } finally {
            try {
                free(sftp);
            } catch (JSchException e) {
                throw new DeleteException(this, e);
            }
        }
        return this;
    }

    @Override
    public SshNode deleteDirectory() throws DirectoryNotFoundException, DeleteException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
        } catch (JSchException e) {
            throw new DeleteException(this, e);
        }
        try {
            sftp.rmdir(escape(slashPath));
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                throw new DirectoryNotFoundException(this);
            }
            throw new DeleteException(this, e);
        } finally {
            try {
                free(sftp);
            } catch (JSchException e) {
                throw new DeleteException(this, e);
            }
        }
        return this;
    }

    @Override
    public SshNode deleteTree() throws DeleteException, NodeNotFoundException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
        } catch (JSchException e) {
            throw new DeleteException(this, e);
        }
        try {
            doDelete(sftp);
        } finally {
            try {
                free(sftp);
            } catch (JSchException e) {
                throw new DeleteException(this, e);
            }
        }
        return this;
    }

    private void doDelete(ChannelSftp sftp) throws DeleteException, NodeNotFoundException {
        SftpATTRS stat;

        try {
            // stat follows symlinks - lstat does *not*. Delete must *not* follow symlinks
            stat = sftp.lstat(escape(slashPath));
            if (stat.isDir()) {
                // http://tools.ietf.org/html/draft-ietf-secsh-filexfer-05 does not mention that the directory has to be empty
                // ... but testing showed that it fails with none-empty directories.
                for (SshNode child : list()) {
                    child.doDelete(sftp);
                }
                sftp.rmdir(escape(slashPath));
            } else {
                sftp.rm(escape(slashPath));
            }
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE || e.id == ChannelSftp.SSH_FX_FAILURE) {
                throw new NodeNotFoundException(this);
            }
            throw new DeleteException(this, e);
        } catch (DirectoryNotFoundException | ListException e) {
            throw new DeleteException(this, e);
        }
    }

    @Override
    public Node move(Node destNode, boolean override) throws MoveException {
        SshNode dest;
        ChannelSftp sftp;

        if (!(destNode instanceof SshNode)) {
            super.move(destNode, override);
        }
        dest = (SshNode) destNode;
        try {
            if (!override) {
                dest.checkNotExists();
            }
            sftp = alloc();
            try {
                sftp.rename(escape(slashPath), escape(dest.slashPath));
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException | IOException e) {
            throw new MoveException(this, dest, "ssh failure", e);
        }
        return dest;
    }

    @Override
    public Node mkdir() throws MkdirException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                // do NOT escape here
                sftp.mkdir(slashPath);
            } finally {
                free(sftp);
            }
            return this;
        } catch (SftpException | JSchException e) {
            throw new MkdirException(this, e);
        }
    }

    private boolean noSuchFile(Exception e) throws ExistsException {
        if (e instanceof SftpException) {
            if (((SftpException) e).id == 2) {
                return false;
            }
        }
        throw new ExistsException(this, e);
    }

    @Override
    public boolean exists() throws ExistsException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                // do not follow links!
                sftp.lstat(escape(slashPath));
            } finally {
                free(sftp);
            }
            return true;
        } catch (SftpException | JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isFile() throws ExistsException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                // follow links!
                return !sftp.stat(escape(slashPath)).isDir();
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                // follow links!
                return sftp.stat(escape(slashPath)).isDir();
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isLink() throws ExistsException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                // CAUTION: use lstat to *not* follow symlinks
                return sftp.lstat(escape(slashPath)).isLink();
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public void mklink(String target) throws LinkException {
        ChannelSftp sftp;

        try {
            checkNotExists();
            getParent().checkDirectory();
            sftp = alloc();
            try {
                sftp.symlink(target, escape(slashPath));
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException | IOException e) {
            throw new LinkException(this, e);
        }
    }

    @Override
    public String readLink() throws ReadLinkException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return sftp.readlink(escape(slashPath));
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            throw new ReadLinkException(this, e);
        } catch (JSchException e) {
            throw new ReadLinkException(this, e);
        }
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return 1000L * sftp.stat(escape(slashPath)).getMTime();
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            throw new GetLastModifiedException(this, e);
        }
    }


    @Override
    public void setLastModified(long millis) throws SetLastModifiedException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                sftp.setMtime(escape(slashPath), (int) (millis / 1000));
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            throw new SetLastModifiedException(this, e);
        }
    }

    @Override
    public String getPermissions() throws ModeException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return toPermissions(sftp.stat(escape(slashPath)).getPermissions() & 0777);
            } finally {
                free(sftp);
            }
        } catch (JSchException | SftpException e) {
            throw new ModeException(this, e);
        }
    }
    @Override
    public void setPermissions(String permissions) throws ModeException {
        SftpATTRS stat;
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                stat = sftp.stat(escape(slashPath));
                stat.setPERMISSIONS(fromPermissions(permissions));
                sftp.setStat(escape(slashPath), stat);
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            throw new ModeException(this, e);
        }
    }


    @Override
    public SshPrincipal getOwner() throws ModeException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return new SshPrincipal(sftp.stat(escape(slashPath)).getUId());
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            throw new ModeException(this, e);
        }
    }

    @Override
    public void setOwner(UserPrincipal owner) throws ModeException {
        String str;
        SftpATTRS stat;
        ChannelSftp sftp;

        try {
            if (isDirectory()) { // TODO
                str = getRoot().exec("chown", owner.getName(), escape(slashPath));
                if (str.length() > 0) {
                    throw new ModeException(this, "chown failed:" + str);
                }
            } else {
                sftp = alloc();
                try {
                    stat = sftp.stat(escape(slashPath));
                    stat.setUIDGID(((SshPrincipal) owner).id, stat.getGId());
                    sftp.setStat(escape(slashPath), stat);
                } finally {
                    free(sftp);
                }
            }
        } catch (ExitCode | NodeException | JSchException | SftpException e) {
            throw new ModeException(this, e);
        }
    }

    @Override
    public GroupPrincipal getGroup() throws ModeException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return new SshPrincipal(sftp.stat(escape(slashPath)).getGId());
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            throw new ModeException(this, e);
        }
    }

    @Override
    public void setGroup(GroupPrincipal group) throws ModeException {
        String str;
        SftpATTRS stat;
        ChannelSftp sftp;

        try {
            if (isDirectory()) { // TODO
                str = getRoot().exec("chgrp", group.getName(), slashPath);
                if (str.length() > 0) {
                    throw new ModeException(this, "chgrp failed:" + str);
                }
            } else {
                sftp = alloc();
                try {
                    stat = sftp.stat(escape(slashPath));
                    stat.setUIDGID(stat.getUId(), ((SshPrincipal) group).id);
                    sftp.setStat(escape(slashPath), stat);
                } finally {
                    free(sftp);
                }
            }
        } catch (NodeException | ExitCode | JSchException | SftpException e) {
            throw new ModeException(this, e);
        }
    }

    @Override
    public byte[] readBytes() throws IOException {
        ByteArrayOutputStream dest;

        dest = new ByteArrayOutputStream();
        writeTo(dest);
        return dest.toByteArray();
    }

    @Override
    public SshNode writeBytes(byte[] bytes, int ofs, int len, boolean append) throws IOException {
        readFrom(new ByteArrayInputStream(bytes, ofs, len), append);
        return this;
    }

    @Override
    public InputStream createInputStream() throws FileNotFoundException, CreateInputStreamException {
        final FileNode tmp;

        try {
            tmp = getWorld().getTemp().createTempFile();
            try (OutputStream dest = tmp.createOutputStream()) {
                writeTo(dest);
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw new CreateInputStreamException(this, e);
        }
        return new FilterInputStream(tmp.createInputStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                // opt because it may be closed twice:
                tmp.deleteTreeOpt();
            }
        };
    }

    @Override
    public OutputStream createOutputStream(final boolean append) throws FileNotFoundException, CreateOutputStreamException {
        try {
            if (isDirectory()) {
                throw new FileNotFoundException(this);
            }
        } catch (ExistsException e) {
            throw new CreateOutputStreamException(this, e);
        }
        return new CheckedByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                readFrom(new ByteArrayInputStream(toByteArray()), append);
            }
        };
    }

    private static class Progress implements SftpProgressMonitor {
        public long sum = 0;

        @Override
        public void init(int op, String src, String dest, long max) {
        }

        @Override
        public boolean count(long count) {
            sum += count;
            return true;
        }

        @Override
        public void end() {
        }
    }

    /**
     * This is the core function to read an ssh node. Does not close dest.
     *
     * @throws FileNotFoundException if this is not a file
     */
    @Override
    public long writeTo(OutputStream dest, long skip) throws FileNotFoundException, WriteToException {
        ChannelSftp sftp;
        Progress monitor;

        try {
            sftp = alloc();
            monitor = new Progress();
            try {
                sftp.get(escape(slashPath), dest, monitor, ChannelSftp.RESUME, skip);
            } finally {
                free(sftp);
            }
            return Math.max(0, monitor.sum - skip);
        } catch (SftpException e) {
            if (e.id == 2 || e.id == 4) {
                throw new FileNotFoundException(this);
            }
            throw new WriteToException(this, e);
        } catch (JSchException e) {
            throw new WriteToException(this, e);
        }
    }

    public void readFrom(InputStream src) throws ReadFromException {
        readFrom(src, false);
    }

    /**
     * This is the core function to write an ssh node. Does not close src.
     *
     * @throws FileNotFoundException if this is not a file
     */
    public void readFrom(InputStream src, boolean append) throws ReadFromException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                sftp.put(src, escape(slashPath), append ? ChannelSftp.APPEND : ChannelSftp.OVERWRITE);
            } finally {
                free(sftp);
            }
        } catch (SftpException | JSchException e) {
            throw new ReadFromException(this, e);
        }
    }


    //-- convenience

    private ChannelSftp alloc() throws JSchException {
        return root.allocateChannelSftp();
    }

    private void free(ChannelSftp channel) throws JSchException {
        root.freeChannelSftp(channel);
    }

    //--

    public static int fromPermissions(String str) {
        int result;

        result = 0;
        for (int i = 0; i < str.length(); i++) {
            result = result << 1;
            if (str.charAt(i) != '-') {
                result |= 1;
            }
        }
        return result;
    }

    public static String toPermissions(int bits) {
        StringBuilder builder;

        builder = new StringBuilder(9);
        builder.append((bits & 0x100) != 0 ? 'r' : '-');
        builder.append((bits & 0x80) != 0 ? 'w' : '-');
        builder.append((bits & 0x40) != 0 ? 'x' : '-');
        builder.append((bits & 0x20) != 0 ? 'r' : '-');
        builder.append((bits & 0x10) != 0 ? 'w' : '-');
        builder.append((bits & 0x08) != 0 ? 'x' : '-');
        builder.append((bits & 0x04) != 0 ? 'r' : '-');
        builder.append((bits & 0x02) != 0 ? 'w' : '-');
        builder.append((bits & 0x01) != 0 ? 'x' : '-');
        return builder.toString();
    }
}
