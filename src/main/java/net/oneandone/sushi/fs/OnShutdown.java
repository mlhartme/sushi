/*
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

import net.oneandone.sushi.fs.file.FileNode;

import java.io.IOException;
import java.nio.file.Files;
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
 * 3) only java.world.File can create a temp file atomically
 */
public class OnShutdown extends Thread {
    /** null if the exit task has already been started */
    private List<FileNode> delete;

    private final String prefix;

    private final List<Runnable> onShutdown;

    public OnShutdown() {
        this.delete = new ArrayList<>();
        this.prefix = "sushitmp" + new SimpleDateFormat("MMdd-HHmm").format(new Date()) + "-"
            + (System.currentTimeMillis() % 100)  + "-";
        this.onShutdown = new ArrayList<>();
    }

    public synchronized void onShutdown(Runnable runnable) {
        onShutdown.add(runnable);
    }

    //--

    /** @throws IOException is dir is not a directory */
    public FileNode createFile(FileNode dir) throws IOException {
        FileNode result;

        result = new FileNode(dir.getRoot(), Files.createTempFile(dir.toPath(), prefix, ".tmp"));
        deleteAtExit(result);
        return result;
    }

    /** @throws IOException is dir is not a directory */
    public FileNode createDirectory(FileNode dir) throws IOException {
        FileNode result;

        result = new FileNode(dir.getRoot(), Files.createTempDirectory(dir.toPath(), prefix));
        deleteAtExit(result);
        return result;
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

    /**
     * Deletes all temp files created until now, without waiting for shutdown - usefull for long-running processes.
     * Caution: the caller has to be sure that none of the files is still in use. It's usually better to explicitly
     * delete temporary files in your application if you're know they are no longer used. */
    public synchronized void deleteNow() {
        for (FileNode node : delete) {
            tryDelete(node);
        }
        delete.clear();
    }

    //--

    /**
     * Use this instead of File.deleteAtExist because it can delete none-empty directories
     * @param node  file or directory
     */
    public synchronized void deleteAtExit(FileNode node) {
        if (delete == null) {
            // already exiting
            tryDelete(node);
        } else {
            delete.add(node);
        }
    }

    /**
     * @param node  file or directory
     */
    public synchronized void dontDeleteAtExit(FileNode node) {
        if (delete == null) {
            throw new IllegalStateException();
        }
        if (!delete.remove(node)) {
            throw new IllegalArgumentException("not registered: " + node.getAbsolute());
        }
    }

    private boolean tryDelete(FileNode node) {
        try {
            if (node.exists()) {
                node.deleteTree();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
