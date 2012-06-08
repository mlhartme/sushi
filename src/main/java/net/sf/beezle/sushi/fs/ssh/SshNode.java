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

package net.sf.beezle.sushi.fs.ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import net.sf.beezle.sushi.fs.DeleteException;
import net.sf.beezle.sushi.fs.ExistsException;
import net.sf.beezle.sushi.fs.GetLastModifiedException;
import net.sf.beezle.sushi.fs.LengthException;
import net.sf.beezle.sushi.fs.LinkException;
import net.sf.beezle.sushi.fs.ListException;
import net.sf.beezle.sushi.fs.MkdirException;
import net.sf.beezle.sushi.fs.MoveException;
import net.sf.beezle.sushi.fs.Node;
import net.sf.beezle.sushi.fs.ReadLinkException;
import net.sf.beezle.sushi.fs.SetLastModifiedException;
import net.sf.beezle.sushi.fs.WriteToException;
import net.sf.beezle.sushi.fs.file.FileNode;
import net.sf.beezle.sushi.io.CheckedByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

        try {
            sftp = alloc();
            try {
                SftpATTRS attrs = sftp.stat(escape(slashPath));
                if (attrs.isDir()) {
                    throw new LengthException(this, new IOException("file expected"));
                }
                return attrs.getSize();
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            throw new LengthException(this, e);
        } catch (JSchException e) {
            throw new LengthException(this, e);
        }
    }

    @Override
    public String getPath() {
        return slashPath.substring(1);
    }

    //--

    @Override
    public List<SshNode> list() throws ListException {
        List<SshNode> nodes;
        ChannelSftp.LsEntry entry;
        String name;
        boolean dir;
        ChannelSftp sftp;

        try {
            nodes = new ArrayList<SshNode>();
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
                throw new ListException(this, new FileNotFoundException(getPath()));
            }
            throw new ListException(this, e);
        } catch (JSchException e) {
            throw new ListException(this, e);
        }
    }

    //--

    @Override
    public SshNode deleteFile() throws DeleteException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
        } catch (JSchException e) {
            throw new DeleteException(this, e);
        }
        try {
            sftp.rmdir(escape(slashPath));
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE || e.id == ChannelSftp.SSH_FX_FAILURE) {
                throw new DeleteException(this, new FileNotFoundException());
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
    public SshNode deleteDirectory() throws DeleteException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
        } catch (JSchException e) {
            throw new DeleteException(this, e);
        }
        try {
            sftp.rm(escape(slashPath));
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE || e.id == ChannelSftp.SSH_FX_FAILURE) {
                throw new DeleteException(this, new FileNotFoundException());
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
    public SshNode deleteTree() throws DeleteException {
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

    private void doDelete(ChannelSftp sftp) throws DeleteException {
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
                throw new DeleteException(this, new FileNotFoundException());
            }
            throw new DeleteException(this, e);
        } catch (ListException e) {
            throw new DeleteException(this, e);
        }
    }

    @Override
    public Node move(Node destNode) throws MoveException {
        SshNode dest;
        ChannelSftp sftp;

        if (!(destNode instanceof SshNode)) {
            throw new MoveException(this, destNode, "target has is different node type");
        }
        dest = (SshNode) destNode;
        try {
            sftp = alloc();
            try {
                sftp.rename(escape(slashPath), escape(dest.slashPath));
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            throw new MoveException(this, dest, "ssh failure", e);
        } catch (JSchException e) {
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
        } catch (SftpException e) {
            throw new MkdirException(this, e);
        } catch (JSchException e) {
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
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
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
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
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
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
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
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
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
        } catch (SftpException e) {
            throw new LinkException(this, e);
        } catch (JSchException e) {
            throw new LinkException(this, e);
        } catch (IOException e) {
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
        } catch (SftpException e) {
            throw new GetLastModifiedException(this, e);
        } catch (JSchException e) {
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
        } catch (SftpException e) {
            throw new SetLastModifiedException(this, e);
        } catch (JSchException e) {
            throw new SetLastModifiedException(this, e);
        }
    }

    @Override
    public int getMode() throws IOException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return sftp.stat(escape(slashPath)).getPermissions() & 0777;
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            throw new IOException(e);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setMode(int mode) throws IOException {
        SftpATTRS stat;
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                stat = sftp.stat(escape(slashPath));
                stat.setPERMISSIONS(mode);
                sftp.setStat(escape(slashPath), stat);
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            throw new IOException(e);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getUid() throws IOException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return sftp.stat(escape(slashPath)).getUId();
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            throw new IOException(e);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setUid(int uid) throws IOException {
        String str;
        SftpATTRS stat;
        ChannelSftp sftp;

        try {
            if (isDirectory()) { // TODO
                str = getRoot().exec("chown", Integer.toString(uid), escape(slashPath));
                if (str.length() > 0) {
                    throw new IOException("chown failed:" + str);
                }
            } else {
                sftp = alloc();
                try {
                    stat = sftp.stat(escape(slashPath));
                    stat.setUIDGID(uid, stat.getGId());
                    sftp.setStat(escape(slashPath), stat);
                } finally {
                    free(sftp);
                }
            }
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getGid() throws IOException {
        ChannelSftp sftp;

        try {
            sftp = alloc();
            try {
                return sftp.stat(escape(slashPath)).getGId();
            } finally {
                free(sftp);
            }
        } catch (SftpException e) {
            throw new IOException(e);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setGid(int gid) throws IOException {
        String str;
        SftpATTRS stat;
        ChannelSftp sftp;

        try {
            if (isDirectory()) { // TODO
                str = getRoot().exec("chgrp", Integer.toString(gid), slashPath);
                if (str.length() > 0) {
                    throw new IOException("chgrp failed:" + str);
                }
            } else {
                sftp = alloc();
                try {
                    stat = sftp.stat(escape(slashPath));
                    stat.setUIDGID(stat.getUId(), gid);
                    sftp.setStat(escape(slashPath), stat);
                } finally {
                    free(sftp);
                }
            }
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (SftpException e) {
            throw new IOException(e);
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
    public InputStream createInputStream() throws IOException {
        final FileNode tmp;
        OutputStream dest;

        tmp = getWorld().getTemp().createTempFile();
        dest = tmp.createOutputStream();
        writeTo(dest);
        dest.close();
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
    public OutputStream createOutputStream(final boolean append) throws IOException {
        return new CheckedByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                try {
                    readFrom(new ByteArrayInputStream(toByteArray()), append);
                } catch (JSchException e) {
                    throw new IOException(e);
                } catch (SftpException e) {
                    throw new IOException(e);
                }
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
     * This is the core funktion to read an ssh node. Does not close out.
     *
     * @throws FileNotFoundException if this is not a file
     */
    public long writeTo(OutputStream dest, long skip) throws WriteToException, FileNotFoundException {
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
                throw new FileNotFoundException(getPath());
            }
            throw new WriteToException(this, e);
        } catch (JSchException e) {
            throw new WriteToException(this, e);
        }
    }

    public void readFrom(InputStream src) throws JSchException, SftpException {
        readFrom(src, false);
    }

    public void readFrom(InputStream src, boolean append) throws JSchException, SftpException {
        ChannelSftp sftp;

        sftp = alloc();
        try {
            sftp.put(src, escape(slashPath), append ? ChannelSftp.APPEND : ChannelSftp.OVERWRITE);
        } finally {
            free(sftp);
        }
    }


    //-- convenience

    private ChannelSftp alloc() throws JSchException {
        return root.allocateChannelSftp();
    }

    private void free(ChannelSftp channel) throws JSchException {
        root.freeChannelSftp(channel);
    }

}
