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

package com.oneandone.sushi.fs.ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.oneandone.sushi.fs.DeleteException;
import com.oneandone.sushi.fs.ExistsException;
import com.oneandone.sushi.fs.GetLastModifiedException;
import com.oneandone.sushi.fs.LengthException;
import com.oneandone.sushi.fs.LinkException;
import com.oneandone.sushi.fs.ListException;
import com.oneandone.sushi.fs.MkdirException;
import com.oneandone.sushi.fs.MoveException;
import com.oneandone.sushi.fs.Node;
import com.oneandone.sushi.fs.ReadLinkException;
import com.oneandone.sushi.fs.SetLastModifiedException;
import com.oneandone.sushi.fs.file.FileNode;
import com.oneandone.sushi.io.CheckedByteArrayOutputStream;

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

    public ChannelSftp getChannel() throws JSchException {
        return root.getChannelFtp();
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
        try {
            SftpATTRS attrs = getChannel().stat(escape(slashPath));
            if (attrs.isDir()) {
                throw new LengthException(this, new IOException("file expected"));
            }
            return attrs.getSize();
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

        try {
            nodes = new ArrayList<SshNode>();
            dir = false;
            for (Object obj : getChannel().ls(escape(slashPath))) {
                try {
                    entry = (ChannelSftp.LsEntry) obj;
                    name = entry.getFilename();
                    if (".".equals(name) || "..".equals(name)) {
                        dir = true;
                    } else {
                        nodes.add((SshNode) join(name));
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
        } catch (SftpException e) {
            throw new ListException(this, e);
        } catch (JSchException e) {
            throw new ListException(this, e);
        }
    }

    //--

    @Override
    public SshNode delete() throws DeleteException {
        SftpATTRS stat;
        ChannelSftp channel;

        try {
            channel = getChannel();
            // stat follows symlinks - lstat does *not*. Delete must *not* follow symlinks
            stat = channel.lstat(escape(slashPath));
            if (stat.isDir()) {
                for (Node child : list()) {
                    child.delete();
                }
                channel.rmdir(escape(slashPath));
            } else {
                channel.rm(escape(slashPath));
            }
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE || e.id == ChannelSftp.SSH_FX_FAILURE) {
                throw new DeleteException(this, new FileNotFoundException());
            }
            throw new DeleteException(this, e);
        } catch (JSchException e) {
            throw new DeleteException(this, e);
        } catch (ListException e) {
            throw new DeleteException(this, e);
        }
        return this;
    }

    @Override
    public Node move(Node destNode) throws MoveException {
        SshNode dest;

        if (!(destNode instanceof SshNode)) {
            throw new MoveException(this, destNode, "target has is different node type");
        }
        dest = (SshNode) destNode;
        try {
            getChannel().rename(escape(slashPath), escape(dest.slashPath));
        } catch (SftpException e) {
            throw new MoveException(this, dest, "ssh failure", e);
        } catch (JSchException e) {
            throw new MoveException(this, dest, "ssh failure", e);
        }
        return dest;
    }

    @Override
    public Node mkdir() throws MkdirException {
        try {
            getChannel().mkdir(escape(slashPath));
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
        try {
            // do not follow links!
            getChannel().lstat(escape(slashPath));
            return true;
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isFile() throws ExistsException {
        try {
            // follow links!
            return !getChannel().stat(escape(slashPath)).isDir();
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isDirectory() throws ExistsException {
        try {
            // follow links!
            return getChannel().stat(escape(slashPath)).isDir();
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public boolean isLink() throws ExistsException {
        try {
            // CAUTION: use lstat to *not* follow symlinks
            return getChannel().lstat(escape(slashPath)).isLink();
        } catch (SftpException e) {
            return noSuchFile(e);
        } catch (JSchException e) {
            return noSuchFile(e);
        }
    }

    @Override
    public void mklink(String target) throws LinkException {
        try {
            checkNotExists();
            getParent().checkDirectory();
            getChannel().symlink(target, escape(slashPath));
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
        try {
            return getChannel().readlink(escape(slashPath));
        } catch (SftpException e) {
            throw new ReadLinkException(this, e);
        } catch (JSchException e) {
            throw new ReadLinkException(this, e);
        }
    }

    @Override
    public long getLastModified() throws GetLastModifiedException {
        try {
            return 1000L * getChannel().stat(escape(slashPath)).getMTime();
        } catch (SftpException e) {
            throw new GetLastModifiedException(this, e);
        } catch (JSchException e) {
            throw new GetLastModifiedException(this, e);
        }
    }


    @Override
    public void setLastModified(long millis) throws SetLastModifiedException {
        try {
            getChannel().setMtime(escape(slashPath), (int) (millis / 1000));
        } catch (SftpException e) {
            throw new SetLastModifiedException(this, e);
        } catch (JSchException e) {
            throw new SetLastModifiedException(this, e);
        }
    }

    @Override
    public int getMode() throws IOException {
        try {
            return getChannel().stat(escape(slashPath)).getPermissions() & 0777;
        } catch (SftpException e) {
            throw new IOException(e);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void setMode(int mode) throws IOException {
        SftpATTRS stat;
        ChannelSftp channel;

        try {
            channel = getChannel();
            stat = channel.stat(escape(slashPath));
            stat.setPERMISSIONS(mode);
            channel.setStat(escape(slashPath), stat);
        } catch (SftpException e) {
            throw new IOException(e);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getUid() throws IOException {
        try {
            return getChannel().stat(escape(slashPath)).getUId();
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
        ChannelSftp channel;

        try {
            if (isDirectory()) { // TODO
                str = getRoot().exec("chown", Integer.toString(uid), escape(slashPath));
                if (str.length() > 0) {
                    throw new IOException("chown failed:" + str);
                }
            } else {
                channel = getChannel();
                stat = channel.stat(escape(slashPath));
                stat.setUIDGID(uid, stat.getGId());
                channel.setStat(escape(slashPath), stat);
            }
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int getGid() throws IOException {
        try {
            return getChannel().stat(escape(slashPath)).getGId();
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
        ChannelSftp channel;

        try {
            if (isDirectory()) { // TODO
                str = getRoot().exec("chgrp", Integer.toString(gid), slashPath);
                if (str.length() > 0) {
                    throw new IOException("chgrp failed:" + str);
                }
            } else {
                channel = getChannel();
                stat = channel.stat(escape(slashPath));
                stat.setUIDGID(stat.getUId(), gid);
                channel.setStat(escape(slashPath), stat);
            }
        } catch (JSchException e) {
            throw new IOException(e);
        } catch (SftpException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] readBytes() throws IOException {
        ByteArrayOutputStream out;

        out = new ByteArrayOutputStream();
        get(out);
        return out.toByteArray();
    }

    @Override
    public InputStream createInputStream() throws IOException {
        final FileNode tmp;
        OutputStream out;

        tmp = getIO().getTemp().createTempFile();
        out = tmp.createOutputStream();
        get(out);
        out.close();
        return new FilterInputStream(tmp.createInputStream()) {
            @Override
            public void close() throws IOException {
                super.close();
                // opt because it may be closed twice:
                tmp.deleteOpt();
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
                    put(toByteArray(), append);
                } catch (JSchException e) {
                    throw new IOException(e);
                } catch (SftpException e) {
                    throw new IOException(e);
                }
            }
        };
    }

    /**
     * This is the core funktion to get a file.
     *
     * @throws FileNotFoundException if this is not a file
     */
    public void get(OutputStream out) throws IOException {
        try {
            getChannel().get(escape(slashPath), out);
        } catch (SftpException e) {
            if (e.id == 2 || e.id == 4) {
                throw new FileNotFoundException(getPath());
            }
            throw new IOException(e);
        } catch (JSchException e) {
            throw new IOException(e);
        }
    }

    public void put(final byte[] data) throws JSchException, IOException, SftpException {
        put(data, false);
    }

    public void put(final byte[] data, boolean append) throws JSchException, IOException, SftpException {
        getChannel().put(new ByteArrayInputStream(data), escape(slashPath), append ? ChannelSftp.APPEND : ChannelSftp.OVERWRITE);
    }
}
