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

package com.oneandone.sushi.fs;

import com.oneandone.sushi.fs.file.FileNode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** 
 * Shutdown hook to delete temporary FileNodes (in particular: directories, because 
 * deleteAtExist is restricted to files) and execute arbirary other tasks.
 * 
 * The implementation is intentionally tied to FileNode, it doesn't support Nodes in general because:
 * 1) I create temp file on disk only - I can't see a use case for other node implementations.   
 * 2) node.delete() is might fail because server connections might already be closed
 * 3) only java.io.File can create a temp file atomically 
 */
public class OnShutdown extends Thread {
    private static OnShutdown singleton;

    /** a static singleton, because I don't want a shutdown hook for every IO instance */
    public static synchronized OnShutdown get() {
        if (singleton == null) {
            singleton = new OnShutdown();
            Runtime.getRuntime().addShutdownHook(singleton);
        }
        return singleton;
    }

    /** null if the exit task has already been started */
    private List<FileNode> delete;

    private final String prefix;

    private final String suffix;
    
    // to generate directory names
    private int dirNo;

    private final List<Runnable> onShutdown;
    
    public OnShutdown() {
        this.delete = new ArrayList<FileNode>();
        this.prefix = "sushi" + new SimpleDateFormat("MMdd-HHmm").format(new Date()) + "-"
            + (System.currentTimeMillis() % 100)  + "-";
        this.suffix = ".tmp";
        this.dirNo = 0;
        this.onShutdown = new ArrayList<Runnable>();
    }

    public synchronized void onShutdown(Runnable runnable) {
        onShutdown.add(runnable);
    }
    
    //--

    public FileNode createFile(FileNode dir) throws IOException {
        FileNode file;
        
        file = new FileNode(dir.getRoot(), File.createTempFile(prefix, suffix, dir.getFile()));
        deleteAtExit(file);
        return file;
    }

    public FileNode createDirectory(FileNode dir) throws IOException {
        FileNode file;
    
        dir.checkDirectory();
        for (; true; dirNo++) {
            file = (FileNode) dir.join(prefix + dirNo + suffix);
            if (!file.exists()) {
                file.mkdir(); // do not catch IOExceptios here -- it might be "disk full" ...
                deleteAtExit(file);
                return file;
            }
        }
    }

    //--
    
    @Override
    public synchronized void run() {
        List<FileNode> tmp;
        
        tmp = delete;
        delete = null;
        for (FileNode node : tmp) {
            tryDelete(node);
        }
        for (Runnable runnable : onShutdown) {
            runnable.run();
        }
    }

    //-- 
    
    /**
     * @param node  file or directory
     */
    private synchronized void deleteAtExit(FileNode node) {
        if (delete == null) {
            // already exiting
            tryDelete(node);
        } else {
            delete.add(node);
        }
    }

    private boolean tryDelete(FileNode node) {
        try {
            if (node.exists()) {
                node.delete();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
